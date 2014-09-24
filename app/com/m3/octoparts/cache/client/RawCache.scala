package com.m3.octoparts.cache.client

import shade.memcached.Codec

import scala.concurrent.Future
import scala.concurrent.duration.Duration

/**
 * A facade for [[shade.memcached.Memcached]]
 *
 * Note: We have a dependency on [[shade.memcached.Codec]], which is not ideal,
 * but we can remove it if/when we decide to migrate away from Shade.
 */
trait RawCache {

  /**
   * Fetches a value from the cache store.
   *
   * @return Some(value) in case the key is available, or None otherwise (doesn't throw exception on key missing)
   */
  def get[T](key: String)(implicit codec: Codec[T]): Future[Option[T]]

  /**
   * Sets a (key, value) in the cache store.
   *
   * The TTL can be Duration.Inf (infinite duration).
   */
  def set[T](key: String, value: T, ttl: Duration)(implicit codec: Codec[T]): Future[Unit]

  /**
   * Shutdown and clean up any resources.
   */
  def close(): Unit
}
