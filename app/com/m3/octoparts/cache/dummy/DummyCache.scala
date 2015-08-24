package com.m3.octoparts.cache.dummy

import com.m3.octoparts.cache.Cache
import com.m3.octoparts.cache.key.CacheKey
import com.twitter.zipkin.gen.Span
import shade.memcached.Codec

import scala.concurrent.Future
import scala.concurrent.duration.Duration

object DummyCache extends Cache {

  def get[T](key: CacheKey)(implicit codec: Codec[T], parentSpan: Span): Future[Option[T]] = Future.successful(None)

  def put[T](key: CacheKey, v: T, ttl: Option[Duration])(implicit codec: Codec[T], parentSpan: Span): Future[Unit] = Future.successful(())

}
