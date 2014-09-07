package com.m3.octoparts.cache

import com.m3.octoparts.aggregator.PartRequestInfo
import com.m3.octoparts.aggregator.service.PartRequestServiceBase
import com.m3.octoparts.cache.client.{ CacheClient, CacheException }
import com.m3.octoparts.cache.directive.{ CacheDirective, CacheDirectiveGenerator }
import com.m3.octoparts.model.PartResponse
import com.m3.octoparts.model.config._
import org.apache.http.HttpStatus
import skinny.logging.Logging
import skinny.util.LTSV

import scala.concurrent.Future

trait PartResponseCachingSupport extends PartRequestServiceBase with Logging {

  import com.m3.octoparts.cache.RichCacheControl._

  def cacheClient: CacheClient

  override def processWithConfig(
    ci: HttpPartConfig, partRequestInfo: PartRequestInfo, params: Map[ShortPartParam, String]): Future[PartResponse] = {

    if (partRequestInfo.noCache || !ci.cacheConfig.cachingEnabled) {
      // noCache or TTL defined but 0 => skip caching
      super.processWithConfig(ci, partRequestInfo, params)
    } else {
      val directive = CacheDirectiveGenerator.generateDirective(ci.partId, params, ci.cacheConfig)
      val futureMaybeFromCache = cacheClient.putIfAbsent(directive)(super.processWithConfig(ci, partRequestInfo, params)).recoverWith {
        case ce: CacheException =>
          ce.getCause match {
            case te: shade.TimeoutException =>
              warn(LTSV.dump("Memcached error" -> "timed out", "cache key" -> ce.key.toString))
            case other =>
              error(LTSV.dump("Memcached error" -> other.getClass.getSimpleName, "cache key" -> ce.key.toString), other)
          }
          super.processWithConfig(ci, partRequestInfo, params)
      }
      futureMaybeFromCache.flatMap {
        partResponse =>
          // at this point, the response may come from cache and be stale.
          if (partResponse.retrievedFromCache && partResponse.cacheControl.hasExpired) {
            revalidate(partResponse, directive, ci, partRequestInfo, params)
          } else {
            Future.successful(partResponse)
          }
      }.map {
        // Replace the ID with the one specified in the current request
        partResponse => partResponse.copy(id = partRequestInfo.partRequestId)
      }
    }
  }

  private[cache] def revalidate(
    partResponse: PartResponse, directive: CacheDirective, ci: HttpPartConfig, partRequestInfo: PartRequestInfo, params: Map[ShortPartParam, String]): Future[PartResponse] = {

    val revalidationParams = partResponse.cacheControl.revalidationHeaders.map {
      case (k, v) => ShortPartParam(outputName = k, paramType = ParamType.Header) -> v
    }
    val revalidationResult = super.processWithConfig(ci, partRequestInfo, params ++ revalidationParams)
    val revalidatedFResp = revalidationResult.map {
      revalidatedPartResponse =>
        selectLatest(revalidatedPartResponse, partResponse)
    }
    revalidatedFResp.onSuccess {
      case latestResponse => cacheClient.saveLater(latestResponse, directive)
    }
    revalidatedFResp
  }

  /**
   * @return The response to use (between the cached one and the new one). Will use new one <=> the new one is not a 304.
   */
  private[cache] def selectLatest(newPartResponse: PartResponse, existingPartResponse: PartResponse): PartResponse = {
    val is304 = newPartResponse.statusCode.fold(false) {
      _.intValue == HttpStatus.SC_NOT_MODIFIED
    }
    if (is304) existingPartResponse else newPartResponse
  }

}
