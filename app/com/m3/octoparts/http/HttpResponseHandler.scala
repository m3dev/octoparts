package com.m3.octoparts.http

import java.net.HttpCookie
import java.nio.charset.Charset

import com.m3.octoparts.model.{ CacheControl, Cookie }
import org.apache.http.client.ResponseHandler
import org.apache.http.client.cache.HeaderConstants
import org.apache.http.entity.ContentType
import org.apache.http.util.EntityUtils
import org.apache.http.{ Header, HttpResponse => ApacheHttpResponse }
import org.joda.time.DateTimeUtils

import scala.collection.convert.Wrappers.JListWrapper
import scala.util.Try

/**
 * Custom HttpResponseHandler that returns a [[HttpResponse]] case class
 */
class HttpResponseHandler(defaultEncoding: Charset) extends ResponseHandler[HttpResponse] {
  /**
   * Given a HttpResponse from the Apache HttpClient lib, returns our nice
   * HttpResponse case class
   *
   * Looks like a big method, but mostly it's mostly just pulling values out of
   * [[ApacheHttpResponse]]
   */
  def handleResponse(apacheResp: ApacheHttpResponse): HttpResponse = {
    val headers = apacheResp.getAllHeaders.toSeq
    val headersSeq = headers.map(h => (h.getName, h.getValue))
    val statusLine = apacheResp.getStatusLine
    val cookies = parseCookieHeaders(apacheResp.getHeaders("Set-Cookie"))
    val entity = Option(apacheResp.getEntity)
    val contentType = entity.map(ContentType.getOrDefault)
    val mimeType = contentType.map(_.getMimeType)
    val charset = contentType.flatMap(cType => Option(cType.getCharset)).map(_.displayName)
    val content = entity.map(EntityUtils.toString(_, defaultEncoding))
    val cacheControl = readCacheControl(apacheResp)

    HttpResponse(
      status = statusLine.getStatusCode,
      message = statusLine.getReasonPhrase,
      headers = headersSeq,
      cookies = cookies,
      mimeType = mimeType,
      charset = charset,
      cacheControl = cacheControl,
      body = content
    )
  }

  private[http] def tryParseCookie(value: String): Seq[HttpCookie] = {
    Try {
      // HttpCookie.parse may throw an IllegalArgumentException
      JListWrapper(HttpCookie.parse(value))
    }.getOrElse(Nil)
  }

  /**
   * Given a sequence of Apache HttpClient lib Headers, returns a sequence of
   * Cookie case class objects
   *
   * Filters the headers based on the "Set-Cookie" name and parses the cookie value
   * using HttpCookie.parse from java.net
   *
   * @param headers Seq Headers
   * @return Seq[Cookie]
   */
  private[http] def parseCookieHeaders(headers: Seq[Header]): Seq[Cookie] = {
    for {
      header <- headers
      cookie <- tryParseCookie(header.getValue)
    } yield {
      Cookie(
        name = cookie.getName,
        value = cookie.getValue,
        maxAge = cookie.getMaxAge,
        discard = cookie.getDiscard,
        secure = cookie.getSecure,
        httpOnly = cookie.isHttpOnly,
        path = Option(cookie.getPath),
        domain = Option(cookie.getDomain)
      )
    }
  }

  /**
   * Parses cache-related headers
   */
  private[http] def readCacheControl(apacheResp: ApacheHttpResponse): CacheControl = {
    val etag = apacheResp.getHeaders(HeaderConstants.ETAG).headOption.map(_.getValue)
    // no need to attempt to parse the last-modified date.
    val lastModified = apacheResp.getHeaders(HeaderConstants.LAST_MODIFIED).headOption.map(_.getValue)
    val cacheControlElements = apacheResp.getHeaders(HeaderConstants.CACHE_CONTROL).flatMap(_.getElements)
    val noStore = cacheControlElements.exists(_.getName == HeaderConstants.CACHE_CONTROL_NO_STORE)
    val noCache = cacheControlElements.exists(_.getName == HeaderConstants.CACHE_CONTROL_NO_CACHE)
    val expiresAt = cacheControlElements.filter(_.getName == HeaderConstants.CACHE_CONTROL_MAX_AGE).flatMap {
      elt =>
        Try {
          val maxAgeSeconds = elt.getValue.toLong
          DateTimeUtils.currentTimeMillis() + maxAgeSeconds * 1000L
        }.toOption
    }.headOption
    CacheControl(noStore, noCache, expiresAt, etag, lastModified)
  }
}

