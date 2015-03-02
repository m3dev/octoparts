package com.m3.octoparts.cache

import com.m3.octoparts.cache.key._
import com.twitter.zipkin.gen.Span
import shade.memcached.Codec

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * A cache whose values are keyed using [[com.m3.octoparts.cache.key.CacheKey]]s.
 *
 * Usually wraps an instance of a [[RawCache]].
 */
trait Cache {

  // TODO shade dependency decoupling

  def get[T](key: CacheKey)(implicit codec: Codec[T], parentSpan: Span): Future[Option[T]]

  def put[T](key: CacheKey, v: T, ttl: Option[Duration])(implicit codec: Codec[T], parentSpan: Span): Future[Unit]

}
