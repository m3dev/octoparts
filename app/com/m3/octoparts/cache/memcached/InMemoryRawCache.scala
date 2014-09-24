package com.m3.octoparts.cache.memcached

import com.m3.octoparts.cache.RawCache
import shade.inmemory.{ InMemoryCache => ShadeInMemoryCache }
import shade.memcached.Codec

import scala.concurrent.duration.Duration
import scala.concurrent.{ ExecutionContext, Future }

/**
 * An in-memory cache that can be used as a drop-in replacement for Memcached
 */
class InMemoryRawCache()(implicit executionContext: ExecutionContext) extends RawCache {
  private val impl = ShadeInMemoryCache(executionContext)

  def get[T](key: String)(implicit codec: Codec[T]): Future[Option[T]] = Future {
    impl.get(key)
  }

  def set[T](key: String, value: T, exp: Duration)(implicit codec: Codec[T]): Future[Unit] = Future {
    impl.set(key, value, exp)
  }

  def close() = impl.close()

}
