package com.m3.octoparts.ws

import com.m3.octoparts.JsonFormats
import com.m3.octoparts.model._
import JsonFormats._
import play.api.libs.json._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.JsValue
import play.api.libs.ws._
import org.mockito.Mockito._
import org.mockito.Matchers._
import play.api.mvc.RequestHeader
import play.api.mvc.Results.EmptyContent
import play.api.test.FakeRequest

import scala.concurrent.Future

class OctoClientSpec extends FunSpec with Matchers with ScalaFutures with MockitoSugar {

  import scala.concurrent.ExecutionContext.Implicits.global

  describe("OctoClientLike") {

    def jsonAsWSResponse(js: JsValue): WSResponse = {
      val mockWSResp = mock[WSResponse]
      when(mockWSResp.json).thenReturn(js)
      when(mockWSResp.status).thenReturn(200)
      mockWSResp
    }

    def statusWSResponse(status: Int): WSResponse = {
      val mockWSResp = mock[WSResponse]
      when(mockWSResp.status).thenReturn(status)
      mockWSResp
    }

    def mockWSHolder(fWSResp: Future[WSResponse]): WSRequestHolder = {
      val mockWS = mock[WSRequestHolder]
      when(mockWS.post(anyObject[JsValue])(anyObject(), anyObject())).thenReturn(fWSResp)
      when(mockWS.post(anyObject[EmptyContent])(anyObject(), anyObject())).thenReturn(fWSResp)
      mockWS
    }

    val mockAggReq = AggregateRequest(
      RequestMeta("hi"),
      Seq(PartRequest("someEndpoint"))
    )

    val mockAggReqEmpty = AggregateRequest(
      RequestMeta("hi"),
      Nil
    )

    val mockAggResp = AggregateResponse(
      ResponseMeta("mock", 1L),
      Nil
    )

    def mockSubject(resp: Future[WSResponse], baseURL: String = "http://bobby.com/") = new OctoClientLike {
      val baseUrl = baseURL
      def wsHolderFor(url: String): WSRequestHolder = mockWSHolder(resp)
      val rescuer = PartialFunction.empty
    }

    describe("#urlFor") {
      val mockWS = jsonAsWSResponse(Json.toJson(mockAggResp))
      val resp = Future.successful(mockWS)

      def verifyUrls(subject: OctoClientLike, slashlessBaseUrl: String): Unit = {
        val invokeUrl = subject.urlFor(subject.Invoke)
        val invalidateCacheUrl = subject.urlFor(subject.InvalidateCache, "hello")
        val invalidateCacheForUrl = subject.urlFor(subject.InvalidateCacheFor, "hello", "userId", "3")
        val invalidateCacheGroupUrl = subject.urlFor(subject.InvalidateCacheGroup, "helloGroup")
        val invalidateCacheGroupForUrl = subject.urlFor(subject.InvalidateCacheGroupFor, "helloGroup", "3")
        invokeUrl should be(s"$slashlessBaseUrl/octoparts/2")
        invalidateCacheUrl should be(s"$slashlessBaseUrl/octoparts/2/cache/invalidate/part/hello")
        invalidateCacheForUrl should be(s"$slashlessBaseUrl/octoparts/2/cache/invalidate/part/hello/userId/3")
        invalidateCacheGroupUrl should be(s"$slashlessBaseUrl/octoparts/2/cache/invalidate/cache-group/helloGroup")
        invalidateCacheGroupForUrl should be(s"$slashlessBaseUrl/octoparts/2/cache/invalidate/cache-group/helloGroup/params/3")
      }

      it("should return correct URLs for trailing-slashed baseUrls") {
        val subject = mockSubject(resp, "http://bobby.com/")
        verifyUrls(subject, "http://bobby.com")
      }

      it("should return correct URLs for non-trailing-slashed baseUrls") {
        val subject = mockSubject(resp, "http://bobby.com")
        verifyUrls(subject, "http://bobby.com")
      }
    }

    describe("#invoke(AggregateRequest)") {

      val mockWS = jsonAsWSResponse(Json.toJson(mockAggResp))
      val resp = Future.successful(mockWS)
      val testee = mockSubject(resp)

      it("should properly deserialise AggregateResponses") {
        whenReady(testee.invoke(mockAggReq)) { r =>
          r.responseMeta.id should be("mock")
        }
      }

      it("should skip sending when AggregateRequests are empty") {
        whenReady(testee.invoke(mockAggReqEmpty)) { r =>
          r.responseMeta.processTime should be(0L)
        }
      }

    }

    describe("#invoke[A]") {

      case class User(id: Option[String])

      // Simple RequestMetaBuilder that works on Tuple2(User, RequestHeader)
      implicit val reqMetaBuilder = RequestMetaBuilder.from[(User, RequestHeader)] { userWithReq =>
        val (user, reqHeader) = userWithReq
        RequestMeta(
          id = reqHeader.id.toString,
          userId = user.id
        )
      }

      val mockWS = jsonAsWSResponse(Json.toJson(mockAggResp))
      val resp = Future.successful(mockWS)
      val testee = mockSubject(resp)

      it("should be callable by resolving an in-scope RequestMetaBuilder") {
        val fResp = testee.invoke((User(Some("hello")), FakeRequest()), Seq(PartRequest("hi")))
        whenReady(fResp) { r =>
          r.responseMeta.id should be("mock")
        }
      }
    }

    describe("URL passed to wsHolderFor") {

      val mockWSH = mockWSHolder(Future.successful(jsonAsWSResponse(Json.toJson(mockAggResp))))
      def mockWSHolderCreator = {
        val creator = mock[Function[String, WSRequestHolder]]
        when(creator.apply(anyString())).thenReturn(mockWSH)
        creator
      }

      def verifyUrlWasPassed(block: OctoClientLike => Future[_])(url: String, howManyTimes: Int = 1): Unit = {
        val wsHolderCreator = mockWSHolderCreator
        val subject = new OctoClientLike {
          val baseUrl = "http://bobby.com"
          def wsHolderFor(url: String): WSRequestHolder = wsHolderCreator.apply(url)
          val rescuer = PartialFunction.empty
        }
        whenReady(block(subject)) { _ =>
          verify(wsHolderCreator, times(howManyTimes)).apply(url)
        }
      }

      it("should be correct for #invoke and #invoke[A]") {
        verifyUrlWasPassed { subject =>
          implicit val reqMetaB = RequestMetaBuilder.from[Int](i => RequestMeta(id = i.toString))
          Future.sequence(Seq(
            subject.invoke(mockAggReq),
            subject.invoke(3, Seq(PartRequest("hi")))))
        }("http://bobby.com/octoparts/2", 2)
      }

      it("should be correct for #invalidateCache") {
        verifyUrlWasPassed(_.invalidateCache("hello"))("http://bobby.com/octoparts/2/cache/invalidate/part/hello")
      }

      it("should be correct for #invalidateCacheFor") {
        verifyUrlWasPassed(_.invalidateCacheFor("hello", "userId", "3"))("http://bobby.com/octoparts/2/cache/invalidate/part/hello/userId/3")
      }

      it("should be correct for #invalidateCacheGroup") {
        verifyUrlWasPassed(_.invalidateCacheGroup("helloGroup"))("http://bobby.com/octoparts/2/cache/invalidate/cache-group/helloGroup")
      }

      it("should be correct for #invalidateCacheGroupFor") {
        verifyUrlWasPassed(_.invalidateCacheGroupFor("helloGroup", "3"))("http://bobby.com/octoparts/2/cache/invalidate/cache-group/helloGroup/params/3")
      }

    }

    describe("#emptyPostOk") {

      describe("response status < 400") {
        it("should return true") {
          (1 until 400) foreach { status =>
            val mockWS = statusWSResponse(status)
            val subject = mockSubject(Future.successful(mockWS))
            whenReady(subject.emptyPostOk("whatever")) { _ should be(true) }
          }
        }
      }

      describe("response status > 400") {
        it("should return true") {
          (400 to 500) foreach { status =>
            val mockWS = statusWSResponse(status)
            val subject = mockSubject(Future.successful(mockWS))
            whenReady(subject.emptyPostOk("whatever")) { _ should be(false) }
          }
        }
      }

    }

  }

  describe("ApiUrl") {

  }

}
