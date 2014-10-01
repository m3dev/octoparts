package com.m3.octoparts.cache

import com.m3.octoparts.aggregator.PartRequestInfo
import com.m3.octoparts.aggregator.service.PartRequestServiceBase
import com.m3.octoparts.cache.directive.{ CacheDirective, CacheDirectiveGenerator }
import com.m3.octoparts.model.PartResponse
import com.m3.octoparts.model.config._
import org.apache.http.HttpStatus
import skinny.logging.Logging
import skinny.util.LTSV
import com.m3.octoparts.cache.RichCacheControl._

import scala.concurrent.Future
import scala.util.control.NonFatal

private[cache] object PartResponseCachingSupport {

  /**
   * @return The response to use (between the cached one and the new one). Will use new one <=> the new one is not a 304.
   */
  private[cache] def selectLatest(newPartResponse: PartResponse, existingPartResponse: PartResponse): PartResponse = {
    val is304 = newPartResponse.statusCode.fold(false) {
      _.intValue == HttpStatus.SC_NOT_MODIFIED
    }
    if (is304) existingPartResponse else newPartResponse
  }

  private[cache] def shouldRevalidate(partResponse: PartResponse): Boolean = {
    partResponse.retrievedFromCache && partResponse.cacheControl.shouldRevalidate
  }
}

trait PartResponseCachingSupport extends PartRequestServiceBase with Logging {
  import PartResponseCachingSupport._

  def cacheOps: CacheOps

  override def processWithConfig(ci: HttpPartConfig,
                                 partRequestInfo: PartRequestInfo,
                                 params: Map[ShortPartParam, Seq[String]]): Future[PartResponse] = {

    if (partRequestInfo.noCache || !ci.cacheConfig.cachingEnabled) {
      // noCache or TTL defined but 0 => skip caching
      super.processWithConfig(ci, partRequestInfo, params)
    } else {
      val directive = CacheDirectiveGenerator.generateDirective(ci.partId, params, ci.cacheConfig)
      val futureMaybeFromCache = cacheOps.putIfAbsent(directive)(super.processWithConfig(ci, partRequestInfo, params)).
        recoverWith(onCacheFailure(ci, partRequestInfo, params))
      futureMaybeFromCache.flatMap {
        partResponse =>
          // at this point, the response may come from cache and be stale.
          if (shouldRevalidate(partResponse)) {
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

  private def onCacheFailure(ci: HttpPartConfig,
                             partRequestInfo: PartRequestInfo,
                             params: Map[ShortPartParam, String]): PartialFunction[Throwable, Future[PartResponse]] = {
    case ce: CacheException => {
      ce.getCause match {
        case te: shade.TimeoutException =>
          warn(LTSV.dump("Memcached error" -> "timed out", "cache key" -> ce.key.toString))
        case other =>
          error(LTSV.dump("Memcached error" -> other.getClass.getSimpleName, "cache key" -> ce.key.toString), other)
      }
      super.processWithConfig(ci, partRequestInfo, params)
    }
    case NonFatal(e) => {
      error(LTSV.dump("Memcached error" -> e.getClass.getSimpleName), e)
      super.processWithConfig(ci, partRequestInfo, params)
    }
  }

  private[cache] def revalidate(partResponse: PartResponse,
                                directive: CacheDirective,
                                ci: HttpPartConfig,
                                partRequestInfo: PartRequestInfo,
                                params: Map[ShortPartParam, Seq[String]]): Future[PartResponse] = {

    val revalidationParams = for {
      (name, value) <- partResponse.cacheControl.revalidationHeaders
    } yield {
      ShortPartParam(name, ParamType.Header) -> Seq(value)
    }
    val revalidationResult = super.processWithConfig(ci, partRequestInfo, params ++ revalidationParams)
    val revalidatedFResp = revalidationResult.map {
      revalidatedPartResponse =>
        selectLatest(revalidatedPartResponse, partResponse)
    }
    revalidatedFResp.onSuccess {
      case latestResponse => cacheOps.saveLater(latestResponse, directive)
    }
    revalidatedFResp
  }
}
