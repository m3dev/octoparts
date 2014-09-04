package com.m3.octoparts.cache.client

import com.m3.octoparts.cache.key._
import shade.memcached.Codec

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Intercepts access to the memcached client so that we can e.g. hash the cache key
 */
trait CacheAccessor {

  // TODO shade dependency decoupling

  def doGet[T](key: CacheKey)(implicit codec: Codec[T]): Future[Option[T]]

  def doPut[T](key: CacheKey, v: T, ttl: Option[Duration])(implicit codec: Codec[T]): Future[Unit]

}
