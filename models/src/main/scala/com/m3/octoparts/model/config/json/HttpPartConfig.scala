package com.m3.octoparts.model.config.json

import java.nio.charset.Charset

import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import com.m3.octoparts.model.HttpMethod
import com.m3.octoparts.model.jackson.HttpMethodType
import com.wordnik.swagger.annotations.ApiModelProperty

import scala.annotation.meta.field
import scala.concurrent.duration.{ Duration, FiniteDuration }

case class HttpPartConfig(
  @(ApiModelProperty @field)(required = true) partId: String,
  @(ApiModelProperty @field)(required = true) owner: String,
  @(ApiModelProperty @field)(required = true) uriToInterpolate: String,
  @(ApiModelProperty @field)(required = false, dataType = "string") description: Option[String],
  @(ApiModelProperty @field)(dataType = "string", allowableValues = "get, post, put, delete, head, patch, options")@JsonScalaEnumeration(classOf[HttpMethodType]) method: HttpMethod.Value,
  @(ApiModelProperty @field)(required = true) hystrixConfig: HystrixConfig,
  @(ApiModelProperty @field)(dataType = "array[integer]") additionalValidStatuses: Set[Int] = Set.empty,
  @(ApiModelProperty @field)(required = true, dataType = "integer", allowableValues = "range[1, Infinity]") httpPoolSize: Int,
  @(ApiModelProperty @field)(required = true, dataType = "integer", allowableValues = "range[0, Infinity]") httpConnectionTimeout: FiniteDuration,
  @(ApiModelProperty @field)(required = true, dataType = "integer", allowableValues = "range[0, Infinity]") httpSocketTimeout: FiniteDuration,
  @(ApiModelProperty @field)(required = true, dataType = "string") httpDefaultEncoding: Charset,
  @(ApiModelProperty @field)(required = false, dataType = "string") httpProxy: Option[String] = None,
  parameters: Set[PartParam] = Set.empty,
  @(ApiModelProperty @field)(dataType = "string", required = false) deprecatedInFavourOf: Option[String] = None,
  cacheGroups: Set[CacheGroup] = Set.empty,
  @(ApiModelProperty @field)(dataType = "integer", required = false, allowableValues = "range[0, Infinity]", value = "in ms") cacheTtl: Option[FiniteDuration] = Some(Duration.Zero),
  alertMailSettings: AlertMailSettings,
  @(ApiModelProperty @field)(required = true) localContentsEnabled: Boolean = false,
  @(ApiModelProperty @field)(dataType = "string", required = false) localContents: Option[String] = None)
