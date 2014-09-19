package com.m3.octoparts.cache.client

import com.m3.octoparts.cache.key._
import shade.memcached.{ Codec, Memcached }

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps
import scala.util.control.NonFatal

class MemcachedAccessor(memcached: Memcached, keyGen: MemcachedKeyGenerator)(implicit executionContext: ExecutionContext)
    extends CacheAccessor {

  /**
   * This value is arbitrarily chosen.
   * We could also use Duration.Inf, which Shade would convert to 365 days,
   * or Duration.Zero, which Memcached would treat as infinite.
   */
  private val VERY_LONG_TTL = 30 days

  private def serializeKey(key: CacheKey) = keyGen.toMemcachedKey(key)

  def doGet[T](key: CacheKey)(implicit codec: Codec[T]): Future[Option[T]] = {
    try {
      memcached.get[T](serializeKey(key)).recoverWith {
        case NonFatal(err) => throw new CacheException(key, err)
      }
    } catch {
      case NonFatal(e) => Future.failed(e)
    }
  }

  def doPut[T](key: CacheKey, v: T, ttl: Option[Duration])(implicit codec: Codec[T]): Future[Unit] = {
    try {
      memcached.set[T](serializeKey(key), v, ttl.getOrElse(VERY_LONG_TTL)).recoverWith {
        case NonFatal(err) => throw new CacheException(key, err)
      }
    } catch {
      case NonFatal(e) => Future.failed(e)
    }
  }

}
