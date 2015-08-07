package com.m3.octoparts.model

import com.wordnik.swagger.annotations.ApiModelProperty

import scala.annotation.meta.field
import scala.beans.BeanProperty
import scala.concurrent.duration.FiniteDuration

/**
 * AggregateRequest contains RequestMeta (which contains extra, common fields like
 * serviceId) and a sequence of PartRequests (which describe the required parts)
 *
 * @param requestMeta RequestMeta
 * @param requests List[PartRequest]
 */
case class AggregateRequest(@(ApiModelProperty @field)(required = true)@BeanProperty requestMeta: RequestMeta,
                            @BeanProperty requests: Seq[PartRequest] = Nil)

case class RequestMeta(@(ApiModelProperty @field)(required = true)@BeanProperty id: String,
                       @(ApiModelProperty @field)(required = false, dataType = "string")@BeanProperty serviceId: Option[String] = None,
                       @(ApiModelProperty @field)(required = false, dataType = "string")@BeanProperty userId: Option[String] = None,
                       @(ApiModelProperty @field)(required = false, dataType = "string")@BeanProperty sessionId: Option[String] = None,
                       @(ApiModelProperty @field)(required = false, dataType = "string")@BeanProperty requestUrl: Option[String] = None,
                       @(ApiModelProperty @field)(required = false, dataType = "string")@BeanProperty userAgent: Option[String] = None,
                       @(ApiModelProperty @field)(required = false, dataType = "integer", value = "in ms")@BeanProperty timeout: Option[FiniteDuration] = None)

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
 * @param params list of parameters. Several parameters can have the same key; within those, order is kept as much a can be.
 */
case class PartRequest(@(ApiModelProperty @field)(required = true)@BeanProperty partId: String,
                       @(ApiModelProperty @field)(required = false, dataType = "string")@BeanProperty id: Option[String] = None,
                       // TODO use a Map[String, Seq[String]] here when Swagger finally supports maps.
                       @BeanProperty params: Seq[PartRequestParam] = Nil)

case class PartRequestParam(@(ApiModelProperty @field)(required = true)@BeanProperty key: String,
                            @(ApiModelProperty @field)(required = true)@BeanProperty value: String)

