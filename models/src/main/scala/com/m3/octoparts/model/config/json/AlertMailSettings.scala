package com.m3.octoparts.model.config.json

import io.swagger.annotations.ApiModelProperty

import scala.annotation.meta.field
import scala.concurrent.duration._

case class AlertMailSettings(
  @(ApiModelProperty @field)(required = true) alertMailsEnabled: Boolean = false,
  @(ApiModelProperty @field)(dataType = "integer", required = false, allowableValues = "range[0, Infinity]") alertAbsoluteThreshold: Option[Int] = None,
  @(ApiModelProperty @field)(dataType = "float", required = false, allowableValues = "range[0, 100]") alertPercentThreshold: Option[Double] = None,
  @(ApiModelProperty @field)(dataType = "integer", required = true, allowableValues = "range[0, Infinity]", value = "in ms") alertInterval: FiniteDuration,
  @(ApiModelProperty @field)(dataType = "string", required = false) alertMailRecipients: Option[String] = None
)

object AlertMailSettings {
  val Off = AlertMailSettings(alertInterval = Duration.Zero)
}
