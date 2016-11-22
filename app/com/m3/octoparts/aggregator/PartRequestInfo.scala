package com.m3.octoparts.aggregator

import com.m3.octoparts.model.{ PartRequest, RequestMeta }

/**
 * All the information needed to request a given part
 *
 * @param requestMeta The common meta info that was supplied to the AggregateRequest
 * @param partRequest Information about the individual part being requested
 */
case class PartRequestInfo(
    requestMeta: RequestMeta,
    partRequest: PartRequest,
    noCache: Boolean = false
) {

  val partRequestId = partRequest.id.getOrElse(partRequest.partId)

}
