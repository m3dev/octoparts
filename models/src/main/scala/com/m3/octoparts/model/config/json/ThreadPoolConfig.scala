package com.m3.octoparts.model.config.json

import com.wordnik.swagger.annotations.ApiModelProperty

import scala.annotation.meta.field

case class ThreadPoolConfig(@(ApiModelProperty @field)(required = true) threadPoolKey: String,
                            @(ApiModelProperty @field)(required = true) coreSize: Int,
                            @(ApiModelProperty @field)(required = true) queueSize: Int)
