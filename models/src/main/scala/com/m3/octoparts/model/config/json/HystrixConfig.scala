package com.m3.octoparts.model.config.json

import com.wordnik.swagger.annotations.ApiModelProperty

import scala.annotation.meta.field
import scala.concurrent.duration.Duration

case class HystrixConfig(@(ApiModelProperty @field)(required = true, dataType = "integer", value = "in ms") timeout: Duration,
                         @(ApiModelProperty @field)(required = true) threadPoolConfig: ThreadPoolConfig,
                         @(ApiModelProperty @field)(required = true) commandKey: String,
                         @(ApiModelProperty @field)(required = true) commandGroupKey: String)