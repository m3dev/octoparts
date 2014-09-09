package com.m3.octoparts.model

import com.wordnik.swagger.annotations.ApiModelProperty

import scala.annotation.meta.field
import scala.beans.{ BeanProperty, BooleanBeanProperty }
import scala.concurrent.duration.Duration

/**
 * An AggregateResponse is the output result version of an AggregateRequest
 *
 * It contains ResponseMeta and a sequence of PartResponses
 *
 * @param responseMeta ResponseMeta
 * @param responses PartResponse
 */
case class AggregateResponse(@(ApiModelProperty @field)(required = true)@BeanProperty responseMeta: ResponseMeta,
                             @BeanProperty responses: Seq[PartResponse] = Nil) {

  @(ApiModelProperty @field)(hidden = true)
  private lazy val partsLookup = responses.map(r => r.id -> r).toMap

  /**
   * Look up a PartResponse by id.
   */
  def findPart(id: String): Option[PartResponse] = partsLookup.get(id)

  /**
   * Look up the content of a PartResponse by id.
   *
   * Note that if the API returned an error response (e.g. a 500 Internal Server Error),
   * then the part contents will probably be some kind of error message, which you probably
   * don't want to show to an end-user.
   *
   * It's safer to call `findPart` and check the HTTP status code before using the contents.
   */
  def findPartContents(id: String): Option[String] = findPart(id).flatMap(_.contents)

}

case class ResponseMeta(@(ApiModelProperty @field)(required = true)@BeanProperty id: String,
                        @(ApiModelProperty @field)(required = true, dataType = "integer", value = "in ms")@BeanProperty processTime: Duration)

/**
 * @param partId same as corresponding partRequest
 * @param id same as corresponding partRequest
 * @param cookies
 * @param statusCode
 * @param mimeType
 * @param charset
 * @param cacheControl
 * @param contents
 * @param errors
 * @param warnings
 * @param retrievedFromCache
 *
 */
case class PartResponse(@(ApiModelProperty @field)(required = true)@BeanProperty partId: String,
                        @(ApiModelProperty @field)(required = true)@BeanProperty id: String,
                        @BeanProperty cookies: Seq[Cookie] = Seq.empty,
                        @(ApiModelProperty @field)(required = false, dataType = "integer")@BeanProperty statusCode: Option[Int] = None,
                        @(ApiModelProperty @field)(required = false, dataType = "string")@BeanProperty mimeType: Option[String] = None,
                        @(ApiModelProperty @field)(required = false, dataType = "string")@BeanProperty charset: Option[String] = None,
                        @BeanProperty cacheControl: CacheControl = CacheControl.NotSet,
                        @(ApiModelProperty @field)(required = false, dataType = "string")@BeanProperty contents: Option[String] = None,
                        @BeanProperty warnings: Seq[String] = Nil,
                        @BeanProperty errors: Seq[String] = Nil,
                        @(ApiModelProperty @field)(required = true)@BooleanBeanProperty retrievedFromCache: Boolean = false)

/**
 * Immutable wrapper for cookies
 *
 * Ideally we should handle no more than these fields
 */
case class Cookie(@(ApiModelProperty @field)(required = true)@BeanProperty name: String,
                  @(ApiModelProperty @field)(required = true)@BeanProperty value: String,
                  @(ApiModelProperty @field)(required = true)@BooleanBeanProperty httpOnly: Boolean,
                  @(ApiModelProperty @field)(required = true)@BooleanBeanProperty secure: Boolean,
                  @(ApiModelProperty @field)(required = true)@BooleanBeanProperty discard: Boolean,
                  @(ApiModelProperty @field)(required = true)@BeanProperty maxAge: Long,
                  @(ApiModelProperty @field)(required = false, dataType = "string")@BeanProperty path: Option[String],
                  @(ApiModelProperty @field)(required = false, dataType = "string")@BeanProperty domain: Option[String])

object CacheControl {
  val NotSet = CacheControl()
}

// Avoiding joda's DateTime to reduce models dependencies
/**
 * @param noStore Indicates that the response was explicitly forbidden from being stored
 * @param noCache Indicates that the response must be validated
 * @param expiresAt java timestamp, indicates until when the response can be used without validation
 * @param etag backend-defined String to be used for validation
 * @param lastModified a date. we do not parse it and use it as-is
 */
case class CacheControl(@(ApiModelProperty @field)(required = true)@BooleanBeanProperty noStore: Boolean = false,
                        @(ApiModelProperty @field)(required = true)@BooleanBeanProperty noCache: Boolean = false,
                        @(ApiModelProperty @field)(required = false, dataType = "integer")@BeanProperty expiresAt: Option[Long] = None,
                        @(ApiModelProperty @field)(required = false, dataType = "string")@BeanProperty etag: Option[String] = None,
                        @(ApiModelProperty @field)(required = false, dataType = "string")@BeanProperty lastModified: Option[String] = None) {

  def canRevalidate = etag.isDefined || lastModified.isDefined
}
