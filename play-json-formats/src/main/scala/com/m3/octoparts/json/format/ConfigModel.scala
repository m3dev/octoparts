package com.m3.octoparts.json.format

import com.m3.octoparts.json.EnumerationHelper
import com.m3.octoparts.model.HttpMethod
import com.m3.octoparts.model.config.ParamType
import com.m3.octoparts.model.config.json._
import play.api.libs.json._

/**
 * Holds ConfigModel [[Format]] type class instances
 */
object ConfigModel {

  import CustomFormatters._

  implicit val threadPoolConfigFormat = Json.format[ThreadPoolConfig]
  implicit val cacheGroupConfigFormat = Json.format[CacheGroup]
  implicit val httpMethodEnumFormat = EnumerationHelper.formats(HttpMethod)
  implicit val paramTypeEnumFormat = EnumerationHelper.formats(ParamType)
  implicit val partParamFormat = Json.format[PartParam]
  implicit val hystrixConfigFormat = Json.format[HystrixConfig]
  implicit val alertMailSettingsFormat = Json.format[AlertMailSettings]
  implicit val httpPartConfigFormat = Json.format[HttpPartConfig]

}