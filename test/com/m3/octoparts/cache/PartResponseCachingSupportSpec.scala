package com.m3.octoparts.cache

import org.scalatest.{ Matchers, FunSpec }
import com.m3.octoparts.cache.dummy.NoCacheClient
import com.m3.octoparts.model.{ PartRequest, RequestMeta, CacheControl, PartResponse }
import com.m3.octoparts.aggregator.service.PartRequestServiceBase
import com.m3.octoparts.model.config.{ ShortPartParam, HttpPartConfig }
import com.m3.octoparts.aggregator.PartRequestInfo
import scala.concurrent.Future
import com.m3.octoparts.support.mocks.ConfigDataMocks
import org.scalatest.concurrent.ScalaFutures

class PartResponseCachingSupportSpec extends FunSpec with Matchers with ScalaFutures with ConfigDataMocks {

  val testee = new PartResponseCachingSupport {
    def executionContext = scala.concurrent.ExecutionContext.global
    def cacheClient = NoCacheClient

    def repository = null

    def handlerFactory = null
  }

  describe("PartResponseCachingSupport#selectLatest") {
    val stalePartResponse = PartResponse(partId = "XXX", id = "YYY", contents = Some("A"), cacheControl = CacheControl(etag = Some("123")))

    it("should use existing one when the new one is a 304") {
      val newPartResponse = PartResponse(partId = stalePartResponse.partId, id = stalePartResponse.id, statusCode = Some(304))
      testee.selectLatest(newPartResponse, stalePartResponse) should be(stalePartResponse)
    }

    it("should use new one when its response status is not 304") {
      val newPartResponse1 = PartResponse(partId = stalePartResponse.partId, id = stalePartResponse.id, contents = Some("B"), statusCode = Some(200))
      testee.selectLatest(newPartResponse1, stalePartResponse) should be(newPartResponse1)
    }

    it("should use new one when its response has no status") {
      val newPartResponse2 = PartResponse(partId = stalePartResponse.partId, id = stalePartResponse.id, contents = Some("B"), statusCode = None)
      testee.selectLatest(newPartResponse2, stalePartResponse) should be(newPartResponse2)
    }
  }

  describe("Custom part ID support") {
    trait MockPartRequestServiceBase extends PartRequestServiceBase {
      override protected def processWithConfig(ci: HttpPartConfig, partRequestInfo: PartRequestInfo, params: Map[ShortPartParam, String]) =
        Future.successful(PartResponse(partId = "partId", id = "old custom ID", contents = Some("response body")))
    }
    val cachingSupport = new MockPartRequestServiceBase with PartResponseCachingSupport {
      def executionContext = scala.concurrent.ExecutionContext.global
      def cacheClient = NoCacheClient
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
