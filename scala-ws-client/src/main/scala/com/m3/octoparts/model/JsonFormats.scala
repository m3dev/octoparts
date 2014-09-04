package com.m3.octoparts.model

import play.api.libs.json.Json

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
}
