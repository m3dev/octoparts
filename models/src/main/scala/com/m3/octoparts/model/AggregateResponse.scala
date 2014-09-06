package com.m3.octoparts.model

import scala.beans.{ BeanProperty, BooleanBeanProperty }

/**
 * An AggregateResponse is the output result version of an AggregateRequest
 *
 * It contains ResponseMeta and a sequence of PartResponses
 *
 * @param responseMeta ResponseMeta
 * @param responses PartResponse
 */
case class AggregateResponse(@BeanProperty responseMeta: ResponseMeta,
                             @BeanProperty responses: Seq[PartResponse]) {

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

case class ResponseMeta(@BeanProperty id: String,
                        @BeanProperty processTime: Long)

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
case class PartResponse(@BeanProperty partId: String,
                        @BeanProperty id: String,
                        @BeanProperty cookies: Seq[Cookie] = Seq.empty,
                        @BeanProperty statusCode: Option[Int] = None,
                        @BeanProperty mimeType: Option[String] = None,
                        @BeanProperty charset: Option[String] = None,
                        @BeanProperty cacheControl: CacheControl = CacheControl.NotSet,
                        @BeanProperty contents: Option[String] = None,
                        @BeanProperty warnings: Seq[String] = Seq.empty,
                        @BeanProperty errors: Seq[String] = Seq.empty,
                        @BooleanBeanProperty retrievedFromCache: Boolean = false)

/**
 * Immutable wrapper for cookies
 *
 * Ideally we should handle no more than these fields
 */
case class Cookie(@BeanProperty name: String,
                  @BeanProperty value: String,
                  @BooleanBeanProperty httpOnly: Boolean,
                  @BooleanBeanProperty secure: Boolean,
                  @BooleanBeanProperty discard: Boolean,
                  @BeanProperty maxAge: Long,
                  @BeanProperty path: Option[String],
                  @BeanProperty domain: Option[String])

object CacheControl {
  val NotSet = CacheControl()
}

// Avoiding joda's DateTime to reduce models dependencies
case class CacheControl(@BooleanBeanProperty noStore: Boolean = false,
                        @BeanProperty expiresAt: Option[Long] = None,
                        @BeanProperty etag: Option[String] = None,
                        @BeanProperty lastModified: Option[String] = None) {

  def canRevalidate = etag.isDefined || lastModified.isDefined
}
