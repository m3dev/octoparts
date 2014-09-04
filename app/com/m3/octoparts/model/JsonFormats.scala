package com.m3.octoparts.model

import com.m3.octoparts.model.config._
import play.api.libs.json._

/**
 * Play JSON formats for the model classes
 */
object JsonFormats {

  implicit val requestMetaFormat = Json.format[RequestMeta]
  implicit val partReqParamFormat = Json.format[PartRequestParam]
  implicit val partReqFormat = Json.format[PartRequest]
  implicit val aggReqFormat = Json.format[AggregateRequest]

  implicit val responseMetaFormat = Json.format[ResponseMeta]
  implicit val cookieFormat = Json.format[Cookie]
  implicit val cacheControlFormat = Json.format[CacheControl]
  implicit val partRespFormat = Json.format[PartResponse]
  implicit val aggRespFormat = Json.format[AggregateResponse]

  implicit val partParamWrites = new Writes[PartParam] {
    def writes(param: PartParam) = Json.obj(
      "id" -> param.id,
      "httpPartConfigId" -> param.httpPartConfigId,
      "required" -> param.required,
      "versioned" -> param.versioned,
      "paramType" -> param.paramType.toString,
      "outputName" -> param.outputName,
      "inputNameOverride" -> param.inputNameOverride,
      "createdAt" -> param.createdAt.toString,
      "updatedAt" -> param.updatedAt.toString
    )
  }

  implicit val httpPartConfigFormat = new Writes[HttpPartConfig] {
    def writes(config: HttpPartConfig) = Json.obj(
      "id" -> config.id,
      "partId" -> config.partId,
      "owner" -> config.owner,
      "uriToInterpolate" -> config.uriToInterpolate,
      "description" -> config.description,
      "method" -> config.method.toString,
      "additionalValidStatuses" -> config.additionalValidStatuses,
      "parameters" -> implicitly[Writes[Set[PartParam]]].writes(config.parameters),
      "deprecatedInFavourOf" -> config.deprecatedInFavourOf,
      "cacheGroups" -> config.cacheGroups.map(_.name),
      "cacheTtl" -> config.cacheTtl.map(_.toMillis),
      "alertMailsEnabled" -> config.alertMailsEnabled,
      "alertAbsoluteThreshold" -> config.alertAbsoluteThreshold,
      "alertPercentThreshold" -> config.alertPercentThreshold,
      "alertInterval" -> config.alertInterval.toMillis,
      "alertMailRecipients" -> config.alertMailRecipients,
      "createdAt" -> config.createdAt.toString,
      "updatedAt" -> config.updatedAt.toString
    )
  }

}
