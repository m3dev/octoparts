package com.m3.octoparts.aggregator.service

import com.m3.octoparts.aggregator.PartRequestInfo
import com.m3.octoparts.model.PartResponse
import com.m3.octoparts.model.config.{ HttpPartConfig, ShortPartParam }
import com.twitter.zipkin.gen.Span
import play.api.http.Status

import scala.concurrent.Future

trait PartResponseLocalContentSupport extends PartRequestServiceBase {

  override def processWithConfig(
    ci:              HttpPartConfig,
    partRequestInfo: PartRequestInfo,
    params:          Map[ShortPartParam, Seq[String]]
  )(implicit parentSpan: Span): Future[PartResponse] = {
    if (ci.localContentsEnabled) {
      Future.successful(createPartResponse(ci, partRequestInfo))
    } else {
      super.processWithConfig(ci, partRequestInfo, params).map { pr =>
        if (pr.statusCode.contains(Status.SERVICE_UNAVAILABLE) && ci.hystrixConfigItem.localContentsAsFallback) createPartResponse(ci, partRequestInfo)
        else pr
      }
    }
  }

  private def createPartResponse(
    ci:              HttpPartConfig,
    partRequestInfo: PartRequestInfo
  ) = PartResponse(
    ci.partId,
    id = partRequestInfo.partRequestId,
    statusCode = Some(Status.NON_AUTHORITATIVE_INFORMATION),
    contents = ci.localContents,
    retrievedFromLocalContents = true
  )
}
