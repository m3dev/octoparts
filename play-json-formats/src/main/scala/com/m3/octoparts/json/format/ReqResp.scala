package com.m3.octoparts.json.format

import com.m3.octoparts.model._
import play.api.libs.json.Json

/**
 * Holds Request and Response [[play.api.libs.json.Format]] type class instances
 */
object ReqResp {

  import Duration.format

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
