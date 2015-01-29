package com.m3.octoparts.aggregator.service

import com.m3.octoparts.aggregator.PartRequestInfo
import com.m3.octoparts.model.PartResponse
import com.m3.octoparts.model.config.{ HttpPartConfig, ShortPartParam }

import scala.concurrent.Future

trait PartResponseLocalContentSupport extends PartRequestServiceBase {

  override def processWithConfig(ci: HttpPartConfig,
                                 partRequestInfo: PartRequestInfo,
                                 params: Map[ShortPartParam, Seq[String]]): Future[PartResponse] = {
    if (ci.localContentsEnabled) {
      Future(createPartResponse(ci, partRequestInfo))
    } else {
      super.processWithConfig(ci, partRequestInfo, params)
    }
  }

  private def createPartResponse(ci: HttpPartConfig,
                                 partRequestInfo: PartRequestInfo) = PartResponse(
    ci.partId,
    id = ci.partId,
    statusCode = Some(200),
    contents = ci.localContents,
    retrievedFromLocalContents = true
  )
}
