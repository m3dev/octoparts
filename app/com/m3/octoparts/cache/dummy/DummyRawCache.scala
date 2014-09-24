package com.m3.octoparts.cache.dummy

import com.m3.octoparts.cache.RawCache
import shade.memcached.Codec

import scala.concurrent.Future
import scala.concurrent.duration.Duration

object DummyRawCache extends RawCache {

  def get[T](key: String)(implicit codec: Codec[T]): Future[Option[T]] = Future.successful(None)

  def set[T](key: String, value: T, exp: Duration)(implicit codec: Codec[T]): Future[Unit] = Future.successful(())

  def close(): Unit = {}

}
