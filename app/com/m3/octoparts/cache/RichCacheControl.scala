package com.m3.octoparts.cache

import com.m3.octoparts.model.CacheControl
import org.apache.http.client.cache.HeaderConstants
import org.joda.time.DateTimeUtils

import scala.language.implicitConversions

object RichCacheControl {
  implicit def apply(cacheControl: CacheControl) = new RichCacheControl(cacheControl)
}

/**
 * Enrich model cacheControl because we do not want joda or httpclient to be dependency of the exported models.
 */
class RichCacheControl(val cacheControl: CacheControl) extends AnyVal {

  /**
   *
   * @return true if there is no max-age header
   */
  def hasExpired = cacheControl.expiresAt.fold(true) {
    _ <= DateTimeUtils.currentTimeMillis()
  }

  def revalidationHeaders = Seq(
    cacheControl.etag.map(HeaderConstants.IF_NONE_MATCH -> _),
    cacheControl.lastModified.map(HeaderConstants.IF_MODIFIED_SINCE -> _)
  ).flatten.toMap

}