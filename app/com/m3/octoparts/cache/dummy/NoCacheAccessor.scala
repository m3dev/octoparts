package com.m3.octoparts.cache.dummy

import com.m3.octoparts.cache.client.CacheAccessor
import com.m3.octoparts.cache.key.CacheKey
import shade.memcached.Codec

import scala.concurrent.Future
import scala.concurrent.duration.Duration

object NoCacheAccessor extends CacheAccessor {
  override def doGet[T](key: CacheKey)(implicit codec: Codec[T]): Future[Option[T]] = Future.successful(None)

  override def doPut[T](key: CacheKey, v: T, ttl: Option[Duration])(implicit codec: Codec[T]): Future[Unit] = Future.successful(())
}
