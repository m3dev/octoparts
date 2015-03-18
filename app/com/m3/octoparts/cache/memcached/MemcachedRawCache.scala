package com.m3.octoparts.cache.memcached

import com.m3.octoparts.cache.RawCache
import com.m3.octoparts.future.RichFutureWithTiming._
import com.twitter.zipkin.gen.Span
import shade.memcached.{Codec, Memcached}

import scala.concurrent.duration.Duration

/**
 * A Memcached implementation of [[RawCache]] using the Shade library.
 */
class MemcachedRawCache(memcached: Memcached) extends RawCache {
  import scala.concurrent.ExecutionContext.Implicits.global

  def get[T](key: String)(implicit codec: Codec[T], parentSpan: Span) = {
    memcached.get(key).measure("CACHE_GET")
  }

  def set[T](key: String, value: T, ttl: Duration)(implicit codec: Codec[T], parentSpan: Span) = {
    memcached.set(key, value, ttl).measure("CACHE_PUT")
  }

  def close() = memcached.close()

}
