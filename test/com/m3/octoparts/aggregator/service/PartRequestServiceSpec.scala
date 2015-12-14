package com.m3.octoparts.aggregator.service

import java.util.concurrent.TimeUnit

import com.beachape.logging.LTSVLogger
import com.beachape.zipkin.services.NoopZipkinService
import com.m3.octoparts.aggregator.PartRequestInfo
import com.m3.octoparts.model._
import com.m3.octoparts.model.config.{ Charset, HttpPartConfig, PartParam }
import com.m3.octoparts.repository.ConfigsRepository
import com.m3.octoparts.support.mocks.HandlerMocks
import com.twitter.zipkin.gen.Span
import org.joda.time.{ DateTime }
import org.mockito.Matchers.{ eq => mockitoEq, _ }
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar

import scala.collection.SortedSet
import scala.concurrent.Future
import scala.concurrent.duration.Duration

class PartRequestServiceSpec
    extends FunSpec
    with Matchers
    with ScalaFutures
    with MockitoSugar
    with HandlerMocks {
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val emptySpan = new Span()
  val repository = mock[ConfigsRepository]
  val config = HttpPartConfig(
    Some(1), "123", "owner", Some("description"), "uri", HttpMethod.Get,
    SortedSet.empty, 1, Duration(1, TimeUnit.SECONDS), Duration(1, TimeUnit.SECONDS), Charset.forName("UTF-8"), None, SortedSet.empty,
    None, None, SortedSet.empty, Some(Duration.Zero), false,
    None, None, Duration(1, TimeUnit.SECONDS), None, false, None, DateTime.now, DateTime.now)

  val proxyDef = Map("test1" -> "http://mrkuntest1:8888", "test2" -> "http://mrkuntest2:8888")

  def pReq(partId: String) = PartRequestInfo(RequestMeta("hi"), PartRequest(partId = partId, id = Some("myId")))
  def pReqWithProxy(partId: String, proxy: Option[String]) = PartRequestInfo(RequestMeta("hi", proxyId = proxy), PartRequest(partId = partId, id = Some("myId")))

  describe("#responseFor") {

    describe("when given a proxy Id") {
      val service = new PartRequestService(repository, mockVoidHttpProxyHandlerFactory, NoopZipkinService, proxyDef)
      val proxyConfig = config.copy(httpProxy = Some("default"))

      it("should not use proxy when no proxy-id is set in both of requestMeta and partConfig") {
        doReturn(Future.successful(Some(config))).when(repository).findConfigByPartId(mockitoEq("123"))(anyObject[Span])
        whenReady(service.responseFor(pReq("123"))) { r =>
          r.contents should be(Some("it worked with proxy None"))
        }
      }

      it("should not override proxy when no proxy-id is set in requestMeta") {
        val proxyConfig = config.copy(httpProxy = Some("default"))
        doReturn(Future.successful(Some(proxyConfig))).when(repository).findConfigByPartId(mockitoEq("123"))(anyObject[Span])
        whenReady(service.responseFor(pReq("123"))) { r =>
          r.contents should be(Some("it worked with proxy Some(default)"))
        }
      }

      it("should return a Future[PartResponse] that is filled by proxy response") {
        doReturn(Future.successful(Some(config))).when(repository).findConfigByPartId(mockitoEq("123"))(anyObject[Span])
        whenReady(service.responseFor(pReqWithProxy("123", Some("test1")))) { r =>
          r.contents should be(Some("it worked with proxy Some(http://mrkuntest1:8888)"))
        }
      }

      it("should override proxy setting in partconfig") {
        doReturn(Future.successful(Some(proxyConfig))).when(repository).findConfigByPartId(mockitoEq("123"))(anyObject[Span])
        whenReady(service.responseFor(pReqWithProxy("123", Some("test1")))) { r =>
          r.contents should be(Some("it worked with proxy Some(http://mrkuntest1:8888)"))
        }
      }

      it("should not override proxy when proxy-id is not found in configuration") {
        doReturn(Future.successful(Some(config))).when(repository).findConfigByPartId(mockitoEq("123"))(anyObject[Span])
        val pReqWithProxy = pReq("123").copy(requestMeta = RequestMeta("hi", proxyId = Some("unknown")))
        whenReady(service.responseFor(pReqWithProxy)) { r =>
          r.contents should be(Some("it worked with proxy None"))
        }
      }
    }

    describe("when given a PartRequest with a partId that is not supported") {
      LTSVLogger.info("warming up logger... without this, 'testOnly' for this spec will fail by timeout of Future")
      it("should return a Future[PartResponse] with an error that mentions that the part Id is not supported") {
        val service = new PartRequestService(repository, mockVoidHttpHandlerFactory, NoopZipkinService)
        doReturn(Future.successful(None)).when(repository).findConfigByPartId(anyObject[String]())(anyObject[Span])
        whenReady(service.responseFor(pReq("randomstuff"))) { r =>
          r.errors should be(Seq(service.unsupportedMsg("randomstuff")))
          r.contents should be(None)
          r.id should be("myId")
        }
      }
    }

    describe("when given a working Id") {
      it("should return a Future[PartResponse] that contains a filled in content field") {
        val service = new PartRequestService(repository, mockVoidHttpHandlerFactory, NoopZipkinService)
        doReturn(Future.successful(Some(config))).when(repository).findConfigByPartId(mockitoEq("123"))(anyObject[Span])
        whenReady(service.responseFor(pReq("123"))) { r =>
          r.errors should be('empty)
          r.contents should be(Some("it worked"))
          r.id should be("myId")
        }
      }
      it("should return a Future[PartResponse] when given a part id for a config that has deprecatedInFavourOf filled in") {
        val service = new PartRequestService(repository, mockVoidHttpHandlerFactory, NoopZipkinService)
        val deprecatedConfig = config.copy(deprecatedInFavourOf = Some("helloWorldPart"))
        doReturn(Future.successful(Some(deprecatedConfig))).when(repository).findConfigByPartId(mockitoEq("123"))(anyObject[Span])
        whenReady(service.responseFor(pReq("123"))) { r =>
          r.warnings should be(Seq(service.deprecationMsg("123", "helloWorldPart")))
          r.contents should be(Some("it worked"))
          r.id should be("myId")
        }
      }
    }

    describe("when given an Id that corresponds to a handler that blows up") {
      it("should return a Future that has the exception in a failure") {
        val service = new PartRequestService(repository, mockErrorHttpHandlerFactory, NoopZipkinService)
        doReturn(Future.successful(Some(config))).when(repository).findConfigByPartId(mockitoEq("123"))(anyObject[Span])
        whenReady(service.responseFor(pReq("123")).failed) { e =>
          e shouldBe a[RuntimeException]
        }
      }
    }
    describe("when given an Id that corresponds to a handler that returns a PartResponse with an errors field filled out") {
      it("should return a Future with a PartResponse that contains that errors sequence") {
        val service = new PartRequestService(repository, mockPartResponseWithErrorHttpHandlerFactory, NoopZipkinService)
        doReturn(Future.successful(Some(config))).when(repository).findConfigByPartId(mockitoEq("123"))(anyObject[Span])
        whenReady(service.responseFor(pReq("123"))) { r =>
          r.errors should be(Seq("SomeException"))
        }
      }
      it("should return a Future with a PartResponse that contains that errors sequence and a deprecation warning if the dependency is deprecated") {
        val service = new PartRequestService(repository, mockPartResponseWithErrorHttpHandlerFactory, NoopZipkinService)
        val deprecatedConfig = config.copy(deprecatedInFavourOf = Some("HelloWorld"))
        doReturn(Future.successful(Some(deprecatedConfig))).when(repository).findConfigByPartId(mockitoEq("123"))(anyObject[Span])
        whenReady(service.responseFor(pReq("123"))) { r =>
          r.errors should be(Seq("SomeException"))
          r.warnings should be(Seq(service.deprecationMsg("123", "HelloWorld")))
        }
      }
    }

  }
}
