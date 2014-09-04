package com.m3.octoparts.model

import scala.beans.BeanProperty

/**
 * AggregateRequest contains RequestMeta (which contains extra, common fields like
 * serviceId) and a sequence of PartRequests (which describe the required parts)
 *
 * @param requestMeta RequestMeta
 * @param requests List[PartRequest]
 */
case class AggregateRequest(@BeanProperty requestMeta: RequestMeta,
                            @BeanProperty requests: Seq[PartRequest])

case class RequestMeta(@BeanProperty id: String,
                       @BeanProperty serviceId: Option[String] = None,
                       @BeanProperty userId: Option[String] = None,
                       @BeanProperty sessionId: Option[String] = None,
                       @BeanProperty requestUrl: Option[String] = None,
                       @BeanProperty userAgent: Option[String] = None,
                       @BeanProperty timeoutMs: Option[Long] = None)

/**
 * A request for a given part. One of more of these can be combined into a single AggregateRequest.
 *
 * Note that if there is a "param" with a "body" key, it will
 * be used as the Body of an Http Request if this part request
 * has an Id that corresponds to an Http endpoint
 *
 * @param partId String that corresponds to a dependency part Id
 * @param id if set, will be passed to the corresponding response.
 *           Useful when an [[AggregateRequest]] contains several [[PartRequest]]s with the same partId
 * @param params params Seq[(String, String)] Unfortunately Swagger does
 *               not support Map types yet so we're using this for now.
 */
case class PartRequest(@BeanProperty partId: String,
                       @BeanProperty id: Option[String] = None,
                       @BeanProperty params: Set[PartRequestParam] = Set.empty)

case class PartRequestParam(@BeanProperty key: String,
                            @BeanProperty value: String)

