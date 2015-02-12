package com.m3.octoparts.aggregator.handler

import java.net.URLEncoder

import com.m3.octoparts.aggregator.PartRequestInfo
import com.m3.octoparts.http.{ HttpClientLike, HttpResponse }
import com.m3.octoparts.hystrix.{ HystrixExecutor, MockHttpClientComponent }
import com.m3.octoparts.model.HttpMethod.Get
import com.m3.octoparts.model.{ PartRequest, RequestMeta, PartResponse }
import com.m3.octoparts.model.config.ParamType._
import com.m3.octoparts.model.config._
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpUriRequest
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Future

class HttpPartRequestHandlerSpec extends FunSpec with Matchers with ScalaFutures {

  private val mockPartId = "mock"
  private val stringToInterpolate = "http://mock.com/${path1}/${path2}"

  private val headerParam1 = ShortPartParam("meta.userId", Header)
  private val pathParam1 = ShortPartParam("path1", Path)
  private val pathParam2 = ShortPartParam("path2", Path)
  private val queryParam1 = ShortPartParam("query1", Query)
  private val queryParam2 = ShortPartParam("query2", Query)

  private val partRequestInfo = PartRequestInfo(
    RequestMeta(id = "hey man that's so meta"),
    partRequest = PartRequest(partId = "foob", id = Some("bande a part"))
  )

  private val handler = handlerWithHttpClient(MockHttpClientComponent.httpClient)

  describe("#buildUri") {

    val completeParamWithArgs = Map(
      headerParam1 -> Seq("1"),
      pathParam1 -> Seq("hi"),
      pathParam2 -> Seq("there"),
      queryParam1 -> Seq("scala"),
      queryParam2 -> Seq("lover", "lover2")
    )

    describe("when providing all params") {
      it("should properly interpolate path and query params") {
        val output = handler.buildUri(completeParamWithArgs)
        output.host shouldBe Some("mock.com")
        output.protocol shouldBe Some("http")
        output.path shouldBe "/hi/there"
        output.query.params(queryParam1.outputName).flatten shouldBe Seq("scala")
        output.query.params(queryParam2.outputName).flatten.toSet shouldBe Set("lover", "lover2")
      }
    }
    describe("when providing only some params") {
      it("should properly ignore missing optional params") {
        val output = handler.buildUri(completeParamWithArgs - queryParam2).toString()
        output should be("http://mock.com/hi/there?query1=scala")
      }
    }
  }

  describe("#process") {
    describe("should asynchronously send an HTTP request") {
      describe("when the HTTP request succeeds with a 200 status code") {
        it("should transform the response to a PartResponse") {
          val client = new HttpClientLike {
            def retrieve(request: HttpUriRequest) = HttpResponse(status = HttpStatus.SC_OK, message = "OK", mimeType = Some("text/plain"), body = Some("hello"))
          }
          val handler = handlerWithHttpClient(client)
          whenReady(handler.process(partRequestInfo, Map.empty)) {
            partResp =>
              partResp should be(PartResponse(partId = mockPartId, id = mockPartId, statusCode = Some(HttpStatus.SC_OK), mimeType = Some("text/plain"), contents = Some("hello"), errors = Nil))
          }
        }
      }
      describe("when the HTTP request fails with a 404 status code") {
        it("should transform the response to a PartResponse with an error") {
          val client = new HttpClientLike {
            def retrieve(request: HttpUriRequest) = HttpResponse(status = HttpStatus.SC_NOT_FOUND, message = "Not Found", mimeType = Some("text/plain"), body = Some("not found"))
          }
          val handler = handlerWithHttpClient(client)
          whenReady(handler.process(partRequestInfo, Map.empty)) {
            partResp =>
              partResp should be(PartResponse(partId = mockPartId, id = mockPartId, statusCode = Some(HttpStatus.SC_NOT_FOUND), mimeType = Some("text/plain"), contents = Some("not found"), errors = Seq("Not Found")))
          }
        }
      }
    }
  }

  describe("#createBlockingHttpRetrieve") {
    describe("when there is no body param") {
      it("should not set a body for the HTTP retrieve") {
        val hArgs = Map(
          ShortPartParam("query1", Query) -> Seq("query1Value")
        )
        handler.createBlockingHttpRetrieve(partRequestInfo, hArgs).maybeBody should be(None)
      }
    }
    describe("when there is a body param") {
      it("should use the body param to set a body for the HTTP retrieve") {
        val hArgs = Map(
          ShortPartParam("jsonPayload", Body) -> Seq("""{"some":"json"}""")
        )
        handler.createBlockingHttpRetrieve(partRequestInfo, hArgs).maybeBody should be(Some("""{"some":"json"}"""))
      }
    }
    it("should include custom HTTP headers for request tracing") {
      val headers = handler.createBlockingHttpRetrieve(partRequestInfo, Map.empty).headers
      headers should contain("X-OCTOPARTS-PART-ID" -> "foob")
      headers should contain("X-OCTOPARTS-REQUEST-ID" -> "bande a part")
      headers should contain("X-OCTOPARTS-PARENT-REQUEST-ID" -> "hey man that's so meta")
    }
  }

  describe("#collectHeaders") {
    it("should collect all headers and cookies into a list of HTTP headers, collapsing the cookies into a single header") {
      val hArgs = Map(
        ShortPartParam("query1", Query) -> Seq("query1Value"),
        ShortPartParam("query2", Query) -> Seq("query2Value"),
        ShortPartParam("header1", Header) -> Seq("header1Value"),
        ShortPartParam("header2", Header) -> Seq("header2Value"),
        ShortPartParam("cookie1", Cookie) -> Seq("cookie1Value"),
        ShortPartParam("cookie2", Cookie) -> Seq("cookie2Value")
      )
      val headers = handler.collectHeaders(hArgs)
      headers should have length 3
      headers should contain("header1" -> "header1Value")
      headers should contain("header2" -> "header2Value")
      headers should contain("Cookie" -> "cookie1=cookie1Value; cookie2=cookie2Value")
    }
    it("should URL-escape cookie names and values") {
      val hArgs = Map(
        ShortPartParam("クッキー１", Cookie) -> Seq("クッキー１の値")
      )
      val headers = handler.collectHeaders(hArgs)
      val expectedCookieString = s"${URLEncoder.encode("クッキー１", "UTF-8")}=${URLEncoder.encode("クッキー１の値", "UTF-8")}"
      headers should contain("Cookie" -> expectedCookieString)
    }
  }

  def handlerWithHttpClient(client: HttpClientLike): HttpPartRequestHandler = {
    new HttpPartRequestHandler {
      def executionContext = scala.concurrent.ExecutionContext.global

      def partId = mockPartId

      def uriToInterpolate = stringToInterpolate

      val hystrixExecutor = new HystrixExecutor(null) {
        override def future[T](f: => T) = Future.successful(f)
      }

      def httpMethod = Get

      val additionalValidStatuses = Set.empty[Int]

      def httpClient = client
    }
  }
}
