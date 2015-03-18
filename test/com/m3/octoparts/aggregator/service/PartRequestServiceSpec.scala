package com.m3.octoparts.aggregator.service

import com.beachape.zipkin.services.NoopZipkinService
import com.twitter.zipkin.gen.Span
import org.mockito.Matchers._
import org.mockito.Matchers.{ eq => mockitoEq }
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar

import com.m3.octoparts.model._
import com.m3.octoparts.repository.ConfigsRepository
import com.m3.octoparts.support.mocks.HandlerMocks
import com.m3.octoparts.model.config.HttpPartConfig
import scala.concurrent.Future
import com.m3.octoparts.aggregator.PartRequestInfo

class PartRequestServiceSpec
    extends FunSpec
    with Matchers
    with ScalaFutures
    with MockitoSugar
    with HandlerMocks {
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val emptySpan = new Span()
  val repository = mock[ConfigsRepository]
  val config = mock[HttpPartConfig]
  doReturn(Set.empty).when(config).parameters
  doReturn(None).when(config).deprecatedInFavourOf
  doReturn("123").when(config).partId

  def pReq(partId: String) = PartRequestInfo(RequestMeta("hi"), PartRequest(partId = partId, id = Some("myId")))

  describe("#responseFor") {

    describe("when given a PartRequest with a partId that is not supported") {
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
        doReturn(Some("helloWorldPart")).when(config).deprecatedInFavourOf
        doReturn(Future.successful(Some(config))).when(repository).findConfigByPartId(mockitoEq("123"))(anyObject[Span])
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
        doReturn(None).when(config).deprecatedInFavourOf
        whenReady(service.responseFor(pReq("123"))) { r =>
          r.errors should be(Seq("SomeException"))
        }
      }
      it("should return a Future with a PartResponse that contains that errors sequence and a deprecation warning if the dependency is deprecated") {
        val service = new PartRequestService(repository, mockPartResponseWithErrorHttpHandlerFactory, NoopZipkinService)
        doReturn(Future.successful(Some(config))).when(repository).findConfigByPartId(mockitoEq("123"))(anyObject[Span])
        doReturn(Some("HelloWorld")).when(config).deprecatedInFavourOf
        whenReady(service.responseFor(pReq("123"))) { r =>
          r.errors should be(Seq("SomeException"))
          r.warnings should be(Seq(service.deprecationMsg("123", "HelloWorld")))
        }
      }
    }

  }
}
