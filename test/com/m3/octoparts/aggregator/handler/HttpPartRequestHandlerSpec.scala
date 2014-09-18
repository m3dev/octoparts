package com.m3.octoparts.aggregator.handler

import com.m3.octoparts.support.db.RequiresDB
import org.scalatest._
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.language.implicitConversions
import com.m3.octoparts.hystrix.{ MockHttpClientComponent, HystrixExecutor }
import com.m3.octoparts.model.HttpMethod.Get
import com.m3.octoparts.model.config._
import com.m3.octoparts.model.config.ParamType._
import com.m3.octoparts.support.mocks.ConfigDataMocks
import org.scalatest.concurrent.ScalaFutures
import com.m3.octoparts.http.{ HttpResponse, HttpClientLike }
import org.apache.http.client.methods.HttpUriRequest
import java.net.URLEncoder
import org.apache.http.HttpStatus
import com.m3.octoparts.model.PartResponse

class HttpPartRequestHandlerSpec extends FunSpec with Matchers with ScalaFutures with ConfigDataMocks with RequiresDB {

  private val mockPartId = "mock"
  private val stringToInterpolate = "http://mock.com/${path1}/${path2}"
  private val mockHystrixArguments = HystrixConfig(
    commandKey = "mock",
    commandGroupKey = "mock",
    timeoutInMs = (10 seconds).toMillis,
    threadPoolConfig = Some(mockThreadConfig),
    updatedAt = now, createdAt = now)

  private val headerParam1 = ShortPartParam("meta.userId", Header)
  private val pathParam1 = ShortPartParam("path1", Path)
  private val pathParam2 = ShortPartParam("path2", Path)
  private val queryParam1 = ShortPartParam("query1", Query)
  private val queryParam2 = ShortPartParam("query2", Query)

  private val handler = handlerWithHttpClient(MockHttpClientComponent.httpClient)

  describe("#buildUri") {

    val completeParamWithArgs = Seq(
      new ShortPartParamValue(headerParam1, "1"),
      new ShortPartParamValue(pathParam1, "hi"),
      new ShortPartParamValue(pathParam2, "there"),
      new ShortPartParamValue(queryParam1, "scala"),
      new ShortPartParamValue(queryParam2, "lover")
    )

    describe("when providing all params") {
      it("should properly interpolate path and query params") {
        val output = handler.buildUri(completeParamWithArgs).toString()
        val baseUrl = "http://mock.com/hi/there"
        // Due to lack of ordering in Maps and Sets, we need to do some hackery here
        Seq(s"$baseUrl?query1=scala&query2=lover", s"$baseUrl?query2=lover&query1=scala").contains(output) should be(true)
      }
    }
    describe("when providing only some params") {
      it("should properly ignore missing optional params") {
        val output = handler.buildUri(completeParamWithArgs.filterNot(_.shortPartParam == queryParam2)).toString()
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
          whenReady(handler.process(Nil)) {
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
          whenReady(handler.process(Nil)) {
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
        val hArgs = Seq(
          new ShortPartParamValue("query1", Query, "query1Value")
        )
        handler.createBlockingHttpRetrieve(hArgs).maybeBody should be(None)
      }
    }
    describe("when there is a body param") {
      it("should use the body param to set a body for the HTTP retrieve") {
        val hArgs = Seq(
          new ShortPartParamValue("jsonPayload", Body, """{"some":"json"}""")
        )
        handler.createBlockingHttpRetrieve(hArgs).maybeBody should be(Some("""{"some":"json"}"""))
      }
    }
  }

  describe("#collectHeaders") {
    it("should collect all headers and cookies into a list of HTTP headers, collapsing the cookies into a single header") {
      val hArgs = Seq(
        new ShortPartParamValue("query1", Query, "query1Value"),
        new ShortPartParamValue("query2", Query, "query2Value"),
        new ShortPartParamValue("header1", Header, "header1Value"),
        new ShortPartParamValue("header2", Header, "header2Value"),
        new ShortPartParamValue("cookie1", Cookie, "cookie1Value"),
        new ShortPartParamValue("cookie2", Cookie, "cookie2Value")
      )
      val headers = handler.collectHeaders(hArgs)
      headers should have length 3
      headers should contain("header1" -> "header1Value")
      headers should contain("header2" -> "header2Value")
      headers should contain("Cookie" -> "cookie1=cookie1Value; cookie2=cookie2Value")
    }
    it("should URL-escape cookie names and values") {
      val hArgs = Seq(
        new ShortPartParamValue("クッキー１", Cookie, "クッキー１の値")
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
      val hystrixExecutor = HystrixExecutor(mockHystrixArguments)
      def httpMethod = Get
      val additionalValidStatuses = Set.empty[Int]
      def httpClient = client
    }
  }
}
