package com.m3.octoparts.model.config.json

import com.wordnik.swagger.annotations.ApiModelProperty

import scala.annotation.meta.field

case class CacheGroup(@(ApiModelProperty @field)(required = true) name: String,
                      @(ApiModelProperty @field)(required = true) owner: String,
                      @(ApiModelProperty @field)(required = true) description: String)
