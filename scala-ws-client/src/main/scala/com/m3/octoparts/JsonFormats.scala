package com.m3.octoparts

import java.util.concurrent.TimeUnit

import com.m3.octoparts.model._
import com.m3.octoparts.model.config.ParamType
import com.m3.octoparts.model.config.json._
import play.api.libs.json._

import scala.concurrent.duration.Duration

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

  implicit val threadPoolConfigFormat = Json.format[ThreadPoolConfig]
  implicit val cacheGroupConfigFormat = Json.format[CacheGroup]

  implicit val durationFormat = new Format[Duration] {
    def writes(o: Duration): JsValue = JsNumber(o.toMillis)
    def reads(json: JsValue): JsResult[Duration] = json match {
      case JsNumber(num) => JsSuccess(Duration(num.toLongExact, TimeUnit.MILLISECONDS))
      case _ => JsError("Invalid num for duration")
    }
  }

  implicit val httpMethodEnumFormat = enumFormat(HttpMethod)
  implicit val paramTypeEnumFormat = enumFormat(ParamType)

  implicit val partParamFormat = Json.format[PartParam]
  implicit val hystrixConfigFormat = Json.format[HystrixConfig]

  implicit val httpPartConfigFormat = Json.format[HttpPartConfig]

  /**
   * Generates a [[Format]] for the given [[Enumeration]]
   */
  def enumFormat[E <: Enumeration](enum: E): Format[E#Value] = {
    Format(enumReads(enum), enumWrites)
  }

  /**
   * Generates a [[Reads]] for the given [[Enumeration]]
   */
  protected def enumReads[E <: Enumeration](enum: E): Reads[E#Value] = new Reads[E#Value] {
    def reads(json: JsValue): JsResult[E#Value] = json match {
      case JsString(s) => {
        try {
          JsSuccess(enum.withName(s))
        } catch {
          case _: NoSuchElementException => JsError(s"Enumeration expected of type: '${enum.getClass}', but it does not appear to contain the value: '$s'")
        }
      }
      case _ => JsError("String value expected")
    }
  }

  /**
   * Generates a [[Writes]] for the [[Enumeration]] type
   */
  protected def enumWrites[E <: Enumeration]: Writes[E#Value] = new Writes[E#Value] {
    def writes(v: E#Value): JsValue = JsString(v.toString)
  }

}
