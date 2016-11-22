package com.m3.octoparts.http

import java.nio.charset.StandardCharsets

import com.google.common.base.Charsets
import org.scalatest.{ BeforeAndAfter, Matchers, FunSpec }
import org.apache.http.message.BasicHttpResponse
import org.apache.http.{ HttpResponse => ApacheHttpResponse, _ }
import org.apache.http.entity.{ ContentType, ByteArrayEntity }
import org.joda.time.{ DateTimeUtils, DateTimeZone, Seconds, DateTime }
import org.joda.time.format.DateTimeFormat
import java.util.Locale
import org.apache.http.protocol.HTTP
import com.m3.octoparts.model.Cookie

class HttpResponseHandlerSpec extends FunSpec with Matchers with BeforeAndAfter {
  private val handler = new HttpResponseHandler(StandardCharsets.UTF_8)

  private def buildApacheResponse(httpResp: HttpResponse, mimeType: String = ContentType.TEXT_PLAIN.getMimeType, charset: String = HTTP.DEF_CONTENT_CHARSET.name()): ApacheHttpResponse = {
    val response = new BasicHttpResponse(new StatusLine {
      override def getProtocolVersion: ProtocolVersion = new ProtocolVersion("com/m3/octoparts/http", 1, 1)

      override def getReasonPhrase: String = httpResp.message

      override def getStatusCode: Int = httpResp.status
    })
    for ((k, v) <- httpResp.headers) {
      response.setHeader(k, v)
    }
    for (body <- httpResp.body) {
      response.setEntity(
        new ByteArrayEntity(body.getBytes(StandardCharsets.UTF_8), ContentType.create(mimeType, charset))
      )
    }

    for (cookie <- httpResp.cookies) {
      //We only test setting values and names
      response.setHeader("Set-Cookie", s"${cookie.name}=${cookie.value}")
    }
    response
  }

  val headers = Seq(
    ("header1", "value1"),
    ("header2", "value2")
  )
  val cookie1 = Cookie("session1", "something", true, true, true, -1, None, None)

  after {
    // Put the system clock back to normal
    DateTimeUtils.setCurrentMillisSystem()
  }

  describe("status parsing") {
    val response = buildApacheResponse(HttpResponse(HttpStatus.SC_CREATED, "CREATED"))
    it("should return the proper status") {
      val httpResponse = handler.handleResponse(response)
      httpResponse.status should be(HttpStatus.SC_CREATED)
      httpResponse.message should be("CREATED")
    }
  }

  describe("basic headers parsing") {
    val response = buildApacheResponse(
      HttpResponse(HttpStatus.SC_OK, "OK", headers = headers, body = Some("hello world")),
      "application/json",
      "UTF-8"
    )
    it("should return all the headers") {
      handler.handleResponse(response).headers should be(headers)
    }

    it("should return the proper mime-type") {
      handler.handleResponse(response).mimeType should be(Some("application/json"))
    }

    it("should return the proper charset ") {
      handler.handleResponse(response).charset should be(Some("UTF-8"))
    }

    it("should deal with a null charset ") {
      val withNullCharset = buildApacheResponse(
        HttpResponse(HttpStatus.SC_OK, "OK", headers = headers, body = Some("hello world")),
        "application/json",
        null
      )
      handler.handleResponse(withNullCharset).charset should be(None)
    }

    it("should return the proper body") {
      handler.handleResponse(response).body should be(Some("hello world"))
    }

    it("should parse a Cache-Control: max-age header") {
      val now = 1401100000000L
      DateTimeUtils.setCurrentMillisFixed(now)
      val headers = Seq(
        "Server" -> "Apache",
        "Cache-Control" -> "max-age=3600, private"
      )
      val response = buildApacheResponse(HttpResponse(HttpStatus.SC_OK, "OK", headers = headers, body = Some("hello world")))

      val result = handler.handleResponse(response)
      result.cacheControl.noStore should be(false)
      result.cacheControl.noCache should be(false)
      result.cacheControl.expiresAt should be(Some(now + 3600 * 1000L))
    }

    it("should parse a Cache-Control: no-store header") {
      val headers = Seq(
        "Server" -> "Apache",
        "Cache-Control" -> "no-store"
      )
      val response = buildApacheResponse(HttpResponse(HttpStatus.SC_OK, "OK", headers = headers, body = Some("hello world")))

      val result = handler.handleResponse(response)
      result.cacheControl.noStore should be(true)
      result.cacheControl.noCache should be(false)
      result.cacheControl.expiresAt should be(None)
    }

    it("should parse a Cache-Control: no-cache header") {
      val headers = Seq(
        "Server" -> "Apache",
        "Cache-Control" -> "no-cache"
      )
      val response = buildApacheResponse(HttpResponse(HttpStatus.SC_OK, "OK", headers = headers, body = Some("hello world")))

      val result = handler.handleResponse(response)
      result.cacheControl.noStore should be(false)
      result.cacheControl.noCache should be(true)
      result.cacheControl.expiresAt should be(None)
    }
  }

  describe("cookies parsing") {
    def createHeaders(kv: Seq[(String, String)]): Seq[Header] = for ((k, v) <- kv) yield {
      new Header {
        def getElements: Array[HeaderElement] = Array.empty

        override def getName: String = k

        override def getValue: String = v
      }
    }

    describe("#handleReponse testing") {
      val response = buildApacheResponse(HttpResponse(HttpStatus.SC_OK, "OK", cookies = Seq(cookie1), headers = headers, body = Some("hello world")))
      it("should return properly set cookies") {
        val cookies = handler.handleResponse(response).cookies
        cookies.head.name should be(cookie1.name)
        cookies.head.value should be(cookie1.value)
      }
    }

    describe("#parseCookieHeaders") {
      // use US locale as in HttpCookie.expiryDate2DeltaSeconds
      val dateTimeFormater = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'").withLocale(Locale.US)
      val cookie1ExpireDateString = "Wed, 13 Jan 2021 22:23:01 GMT"
      val cookieSet1 = ("Set-Cookie", s"value1=what1; Domain=foo.com; Path=/; Expires=$cookie1ExpireDateString; Secure; HttpOnly")
      val cookieSet2 = ("Set-Cookie", "value2=what2; Domain=bar.com; Path=/thing")
      val irrelevantHeader = ("hmm", "errr")
      val headers = createHeaders(Seq(cookieSet1, cookieSet2, irrelevantHeader))

      it("should ignore headers that do not have 'Set-Cookie' as their names") {
        handler.parseCookieHeaders(headers).size should be(2)
      }

      it("should parse the cookies properly") {
        val cookies = handler.parseCookieHeaders(headers)
        val cookie1 = cookies.find(_.name == "value1").get
        val cookie2 = cookies.find(_.name == "value2").get
        cookie1.domain should be(Some("foo.com"))
        cookie1.path should be(Some("/"))
        cookie1.secure should be(true)
        cookie1.httpOnly should be(true)
        val maxAgeExpected1 = Seconds.secondsBetween(DateTime.now.withZone(DateTimeZone.UTC), dateTimeFormater.withZoneUTC.parseDateTime(cookie1ExpireDateString)).getSeconds
        Math.abs(maxAgeExpected1 - cookie1.maxAge) should be < 5L // Within a day
        cookie2.domain should be(Some("bar.com"))
        cookie2.path should be(Some("/thing"))
        cookie2.secure should be(false)
        cookie2.httpOnly should be(false)
        cookie2.maxAge should be(-1) // Not set defaults to -1
      }
    }

  }

}
