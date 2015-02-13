package com.m3.octoparts.http

import com.m3.octoparts.model.{ CacheControl, Cookie }

/**
 * This file contains case classes that serve to wrap the responses
 * that come back from Apache HttpClient so that they are
 * easier to handle
 */

/**
 * Immutable wrapper for Apache HttpClient's [[org.apache.http.HttpResponse]] class
 *
 * @param status status of the HttpResponse
 * @param headers headers Map
 * @param cookies maybe a Sequence of cookies
 * @param mimeType maybe a MimeType
 * @param charset maybe a Charset
 * @param fromFallback whether or not this HTTP response from a fallback
 * @param body maybe a body String
 */
case class HttpResponse(
  status: Int,
  message: String,
  headers: Seq[(String, String)] = Seq.empty,
  cookies: Seq[Cookie] = Seq.empty,
  mimeType: Option[String] = None,
  charset: Option[String] = None,
  cacheControl: CacheControl = CacheControl.NotSet,
  fromFallback: Boolean = false,
  body: Option[String] = None)