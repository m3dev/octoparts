package com.m3.octoparts.aggregator.service

import com.m3.octoparts.aggregator.PartRequestInfo
import com.m3.octoparts.future.RichFutureWithTimeout._
import com.m3.octoparts.future.RichFutureWithTiming._
import com.m3.octoparts.logging.{ LogUtil, PartRequestLogger }
import com.beachape.logging.LTSVLogger
import com.m3.octoparts.model.{ AggregateResponse, PartResponse, ResponseMeta, _ }

import scala.concurrent._
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Service that has a #processParts method that returns a Future[AggregateResponse]
 */
class PartsService(partRequestService: PartRequestServiceBase,
                   val partRequestLogger: PartRequestLogger = PartRequestLogger,
                   maximumAggReqTimeout: FiniteDuration = 5.seconds)(implicit val executionContext: ExecutionContext)
    extends PartServiceErrorHandler with LogUtil {

  import com.m3.octoparts.logging.LTSVables._

  /**
   * Given an AggregateRequest, returns a Future[AggregateResponse]
   *
   * Breaks the AggregateRequest down into individual PartRequestInfos that get processed by the
   * underlying PartRequestService's responseFor method. This is also where we recover
   * from errors thrown in those individual futures.
   *
   * In the event that an AggregateRequest has a RequestMeta that has its timeoutInMs field
   * filled out as a Some[Int], the AggregateRequest is processed with a timeout that is the lesser
   * of timeoutInMs and maximumAggReqTimeout.
   *
   * @param aggregateRequest AggregateRequest
   * @return Future[AggregateResponse]
   */
  def processParts(aggregateRequest: AggregateRequest, noCache: Boolean = false): Future[AggregateResponse] = {
    val requestMeta = aggregateRequest.requestMeta
    val aReqTimeout: FiniteDuration = requestMeta.timeout.getOrElse(maximumAggReqTimeout) min maximumAggReqTimeout
    val partsResponsesFutures = aggregateRequest.requests.map {
      pReq =>
        val partRequestInfo = PartRequestInfo(requestMeta, pReq, noCache)
        recoverChain(partRequestInfo, aReqTimeout, partRequestService.responseFor(partRequestInfo)
          /*
            Return transformed version of the serviceResponse Future that has a timeout and recover block.
            Note:
            - timeoutIn is an Enrichment method for Future that comes from com.m3.octoparts.futures.RichFutureWithTimeout
            - time is an Enrichment method for Future that comes from com.m3.octoparts.futures.RichFutureWithTiming
          */
          .timeoutIn(aReqTimeout)
          .time {
            (partResponse, duration) =>
              LTSVLogger.debug((requestMeta, partResponse), "Time taken" -> toRelevantUnit(duration))
              logPartResponse(requestMeta, partResponse, duration.toMillis)
          })
    }
    Future.sequence(partsResponsesFutures).timeAndTransform {
      (partsResponses, duration) =>
        val responseMeta = ResponseMeta(requestMeta.id, duration)
        val aggregateResponse = AggregateResponse(responseMeta, partsResponses)

        LTSVLogger.debug((aggregateRequest, aggregateResponse))

        aggregateResponse
    }
  }

  private def logPartResponse(requestMeta: RequestMeta, partResponse: PartResponse, responseMs: Long): Unit = partResponse match {
    case pResp if pResp.errors.nonEmpty => partRequestLogger.logFailure(pResp.partId, requestMeta.id, requestMeta.serviceId, pResp.statusCode)
    case pResp if pResp.retrievedFromCache => partRequestLogger.logSuccess(pResp.partId, requestMeta.id, requestMeta.serviceId, cacheHit = true, responseMs)
    case pResp => partRequestLogger.logSuccess(pResp.partId, requestMeta.id, requestMeta.serviceId, cacheHit = false, responseMs)
  }
}
