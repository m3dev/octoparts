package com.m3.octoparts.cache

import com.beachape.logging.LTSVLogger
import com.m3.octoparts.aggregator.PartRequestInfo
import com.m3.octoparts.aggregator.service.PartRequestServiceBase
import com.m3.octoparts.cache.directive.{ CacheDirective, CacheDirectiveGenerator }
import com.m3.octoparts.model.PartResponse
import com.m3.octoparts.model.config._
import com.netflix.hystrix.exception.HystrixRuntimeException
import com.twitter.zipkin.gen.Span
import org.apache.http.HttpStatus
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

trait PartResponseCachingSupport extends PartRequestServiceBase {
  import PartResponseCachingSupport._
  import com.beachape.zipkin.FutureEnrichment._
  def cacheOps: CacheOps

  override def processWithConfig(
    ci:              HttpPartConfig,
    partRequestInfo: PartRequestInfo,
    params:          Map[ShortPartParam, Seq[String]]
  )(implicit parentSpan: Span): Future[PartResponse] = {

    if (partRequestInfo.noCache || !ci.cacheConfig.cachingEnabled) {
      // noCache or TTL defined but 0 => skip caching
      super.processWithConfig(ci, partRequestInfo, params)
    } else {
      val directive = CacheDirectiveGenerator.generateDirective(ci.partId, params, ci.cacheConfig)
      val futureMaybeFromCache =
        cacheOps.putIfAbsent(directive)(super.processWithConfig(ci, partRequestInfo, params))
          .recoverWith(onCacheFailure(ci, partRequestInfo, params))
          .trace("retrieve-part-response-from-cache-or-else", "partId" -> ci.partId)
      futureMaybeFromCache.flatMap {
        partResponse =>
          // at this point, the response may come from cache and be stale.
          if (shouldRevalidate(partResponse)) {
            revalidate(partResponse, directive, ci, partRequestInfo, params).trace("part-response-cache-revalidation")
          } else {
            Future.successful(partResponse)
          }
      }.map {
        // Replace the ID with the one specified in the current request
        partResponse => partResponse.copy(id = partRequestInfo.partRequestId)
      }
    }
  }

  private def onCacheFailure(
    ci:              HttpPartConfig,
    partRequestInfo: PartRequestInfo,
    params:          Map[ShortPartParam, Seq[String]]
  )(implicit parentSpan: Span): PartialFunction[Throwable, Future[PartResponse]] = {
    case ce: CacheException => {
      ce.getCause match {
        case te: shade.TimeoutException =>
          LTSVLogger.warn("Memcached error" -> "timed out", "cache key" -> ce.key.toString)
        case other =>
          LTSVLogger.error(other, "Memcached error" -> other.getClass.getSimpleName, "cache key" -> ce.key.toString)
      }
      super.processWithConfig(ci, partRequestInfo, params)
    }
    case e: HystrixRuntimeException => {
      LTSVLogger.warn(e)
      Future.failed(e) // Don't retry on HystrixRuntime exception
    }
    case NonFatal(e) => {
      LTSVLogger.error(e)
      super.processWithConfig(ci, partRequestInfo, params) // Unknown error, retry
    }
  }

  private[cache] def revalidate(
    partResponse:    PartResponse,
    directive:       CacheDirective,
    ci:              HttpPartConfig,
    partRequestInfo: PartRequestInfo,
    params:          Map[ShortPartParam, Seq[String]]
  )(implicit parentSpan: Span): Future[PartResponse] = {

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
