package com.m3.octoparts.cache.memcached

import com.m3.octoparts.cache.RawCache
import shade.memcached.{ Memcached, Codec }

import scala.concurrent.duration.Duration

/**
 * A Memcached implementation of [[RawCache]] using the Shade library.
 */
class MemcachedRawCache(memcached: Memcached) extends RawCache {

  def get[T](key: String)(implicit codec: Codec[T]) = memcached.get(key)

  def set[T](key: String, value: T, ttl: Duration)(implicit codec: Codec[T]) = memcached.set(key, value, ttl)

  def close() = memcached.close()

}
