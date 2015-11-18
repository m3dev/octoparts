package com.m3.octoparts.cache

import com.beachape.zipkin.services.{ ZipkinServiceLike, NoopZipkinService }
import com.m3.octoparts.aggregator.handler.HttpHandlerFactory
import com.m3.octoparts.cache.key.HttpPartConfigCacheKey
import com.m3.octoparts.repository.ConfigsRepository
import com.netflix.hystrix.HystrixCommand
import com.netflix.hystrix.exception.HystrixRuntimeException
import com.netflix.hystrix.exception.HystrixRuntimeException.FailureType
import com.twitter.zipkin.gen.Span
import org.joda.time.DateTime
import org.scalatest.{ Matchers, FunSpec }
import com.m3.octoparts.cache.dummy.DummyCacheOps
import com.m3.octoparts.model.{ PartRequest, RequestMeta, CacheControl, PartResponse }
import com.m3.octoparts.aggregator.service.PartRequestServiceBase
import com.m3.octoparts.model.config._
import com.m3.octoparts.aggregator.PartRequestInfo
import scala.concurrent.{ ExecutionContext, Future }
import com.m3.octoparts.support.mocks.ConfigDataMocks
import org.scalatest.concurrent.{ IntegrationPatience, Eventually, ScalaFutures }

class PartResponseCachingSupportSpec
    extends FunSpec
    with Matchers
    with ScalaFutures
    with ConfigDataMocks
    with Eventually
    with IntegrationPatience {

  implicit val emptySpan = new Span()

  describe("#processWithConfig") {

    class Super(processOverride: => Future[PartResponse]) extends PartRequestServiceBase {
      def handlerFactory: HttpHandlerFactory = ???
      def repository: ConfigsRepository = ???

      implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.global

      override protected def processWithConfig(ci: HttpPartConfig, partRequestInfo: PartRequestInfo, params: Map[ShortPartParam, Seq[String]])(implicit parentSpan: Span): Future[PartResponse] = {
        processOverride
      }
      protected implicit val zipkinService: ZipkinServiceLike = NoopZipkinService
    }

    def newSubject(process: => Future[PartResponse]) = new Super(process) with PartResponseCachingSupport {
      val cacheOps: CacheOps = DummyCacheOps
    }

    it("should retry on a generic exception") {
      var timesRun = 0
      val subject = newSubject(Future.failed {
        timesRun = timesRun + 1
        new IllegalArgumentException
      })
      whenReady(subject.processWithConfig(mockHttpPartConfig, mockPartRequestInfo, Map.empty).failed) { _ =>
        eventually(timesRun shouldBe 2)
      }

    }

    it("should retry on CacheException") {
      var timesRun = 0
      val subject = newSubject(Future.failed {
        timesRun = timesRun + 1
        new CacheException(HttpPartConfigCacheKey("random"), new IllegalAccessError)
      })
      whenReady(subject.processWithConfig(mockHttpPartConfig, mockPartRequestInfo, Map.empty).failed) { _ =>
        eventually(timesRun shouldBe 2)
      }

    }

    it("should not retry on a HystrixRuntimeException") {
      var timesRun = 0
      val subject = newSubject(Future.failed {
        timesRun = timesRun + 1
        val ex = new HystrixRuntimeException(null, null, null, null, null)
        ex
      })
      whenReady(subject.processWithConfig(mockHttpPartConfig, mockPartRequestInfo, Map.empty).failed) { _ =>
        eventually(timesRun shouldBe 1)
      }

    }
  }

  describe("PartResponseCachingSupport#selectLatest") {
    val stalePartResponse = PartResponse(partId = "XXX", id = "YYY", contents = Some("A"), cacheControl = CacheControl(etag = Some("123")))

    it("should use existing one when the new one is a 304") {
      val newPartResponse = PartResponse(partId = stalePartResponse.partId, id = stalePartResponse.id, statusCode = Some(304))
      PartResponseCachingSupport.selectLatest(newPartResponse, stalePartResponse) should be(stalePartResponse)
    }

    it("should use new one when its response status is not 304") {
      val newPartResponse1 = PartResponse(partId = stalePartResponse.partId, id = stalePartResponse.id, contents = Some("B"), statusCode = Some(200))
      PartResponseCachingSupport.selectLatest(newPartResponse1, stalePartResponse) should be(newPartResponse1)
    }

    it("should use new one when its response has no status") {
      val newPartResponse2 = PartResponse(partId = stalePartResponse.partId, id = stalePartResponse.id, contents = Some("B"), statusCode = None)
      PartResponseCachingSupport.selectLatest(newPartResponse2, stalePartResponse) should be(newPartResponse2)
    }
  }

  describe("PartResponseCachingSupport#shouldRevalidate") {
    it("should never revalidate items that do not come from the cache") {
      PartResponseCachingSupport.shouldRevalidate(PartResponse("partId", "id", retrievedFromCache = false)) should be(false)
    }
    it("should revalidate items that have the no-cache header") {
      PartResponseCachingSupport.shouldRevalidate(PartResponse("partId", "id", retrievedFromCache = true, cacheControl = CacheControl(noCache = true))) should be(true)
    }
    describe("when ETag is NOT present") {
      describe("when max-age is NOT present") {
        it("should NOT revalidate the cached response") {
          val partResponse = PartResponse("partId", "id", retrievedFromCache = true,
            cacheControl = CacheControl(expiresAt = None, etag = None))
          PartResponseCachingSupport.shouldRevalidate(partResponse) should be(false)
        }
      }
      describe("when max-age is present and has NOT expired") {
        it("should NOT revalidate the cached response") {
          val partResponse = PartResponse("partId", "id", retrievedFromCache = true,
            cacheControl = CacheControl(expiresAt = Some(DateTime.now().plusDays(1).getMillis), etag = None))
          PartResponseCachingSupport.shouldRevalidate(partResponse) should be(false)
        }
      }
      describe("when max-age is present and has expired") {
        it("should NOT revalidate the cached response") {
          /*
          Note: actually this case should never happen. Memcached TTL will be <= max-age,
          so the response will be evicted from Memcached before it expires.
           */
          val partResponse = PartResponse("partId", "id", retrievedFromCache = true,
            cacheControl = CacheControl(expiresAt = Some(DateTime.now().minusDays(1).getMillis), etag = None))
          PartResponseCachingSupport.shouldRevalidate(partResponse) should be(false)
        }
      }
    }

    describe("when ETag is present") {
      describe("when max-age is NOT present") {
        it("should revalidate the cached response") {
          val partResponse = PartResponse("partId", "id", retrievedFromCache = true,
            cacheControl = CacheControl(expiresAt = None, etag = Some("abc")))
          PartResponseCachingSupport.shouldRevalidate(partResponse) should be(true)
        }
      }
      describe("when max-age is present and has NOT expired") {
        it("should NOT revalidate the cached response") {
          val partResponse = PartResponse("partId", "id", retrievedFromCache = true,
            cacheControl = CacheControl(expiresAt = Some(DateTime.now().plusDays(1).getMillis), etag = Some("abc")))
          PartResponseCachingSupport.shouldRevalidate(partResponse) should be(false)
        }
      }
      describe("when max-age is present and has expired") {
        it("should NOT revalidate the cached response") {
          val partResponse = PartResponse("partId", "id", retrievedFromCache = true,
            cacheControl = CacheControl(expiresAt = Some(DateTime.now().plusDays(1).getMillis), etag = Some("abc")))
          PartResponseCachingSupport.shouldRevalidate(partResponse) should be(false)
        }

      }
    }
  }

  describe("Custom part ID support") {
    trait MockPartRequestServiceBase extends PartRequestServiceBase {
      override protected def processWithConfig(ci: HttpPartConfig, partRequestInfo: PartRequestInfo, params: Map[ShortPartParam, Seq[String]])(implicit span: Span) =
        Future.successful(PartResponse(partId = "partId", id = "old custom ID", contents = Some("response body")))
    }
    val cachingSupport = new MockPartRequestServiceBase with PartResponseCachingSupport {
      implicit val zipkinService = NoopZipkinService
      def executionContext = scala.concurrent.ExecutionContext.global
      def cacheOps = DummyCacheOps
      def handlerFactory = ???
      def repository = ???
    }

    it("should replace the cached response's ID with the one specified by the client") {
      val partRequestInfo = PartRequestInfo(RequestMeta("foo"), PartRequest(partId = "partId", id = Some("new custom ID")))
      val fResponse = cachingSupport.processWithConfig(mockHttpPartConfig, partRequestInfo, Map.empty)
      whenReady(fResponse) { resp =>
        resp.id should be("new custom ID")
      }
    }
  }
}
