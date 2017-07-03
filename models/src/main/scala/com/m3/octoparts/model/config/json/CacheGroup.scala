package com.m3.octoparts.model.config.json

import io.swagger.annotations.ApiModelProperty

import scala.annotation.meta.field

case class CacheGroup(
  @(ApiModelProperty @field)(required = true) name: String,
  @(ApiModelProperty @field)(required = true) owner: String,
  @(ApiModelProperty @field)(required = true) description: String
)

object CacheGroup {
  implicit val order: Ordering[CacheGroup] = Ordering.by(_.name)
}
