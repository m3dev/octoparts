package com.m3.octoparts.cache.client

import com.m3.octoparts.cache.key.CacheKey
import play.api.Logger
import shade.memcached.Codec

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * A decorator for [[CacheAccessor]] that handles the special case of a ttl shorter than 1 second.
 * If the TTL is less than one second, we should not perform a cache insert, because:
 *  - there is no point, as the element has already expired, or will do very soon
 *  - The TTL will get rounded down to 0, which Memcached treats as meaning "infinite". This is the exact opposite to what we want.
 */
trait SkippingZeroTTL extends CacheAccessor {

  abstract override def doPut[T](key: CacheKey, v: T, ttl: Option[Duration])(implicit codec: Codec[T]): Future[Unit] = ttl match {
    case Some(duration) if duration < 1.second =>
      Logger.debug(s"Skipping cache PUT of $key because ttl ($duration) is less than 1 second")
      Future.successful(())
    case _ => super.doPut(key, v, ttl)
  }

}

