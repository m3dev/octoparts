package com.m3.octoparts.cache.dummy

import shade.memcached.{ Codec, Memcached }

import scala.concurrent.Future
import scala.concurrent.duration.Duration

object NoCacheAdapter extends Memcached {
  override def add[T](key: String, value: T, exp: Duration)(implicit codec: Codec[T]): Future[Boolean] = Future.successful(false)

  override def set[T](key: String, value: T, exp: Duration)(implicit codec: Codec[T]): Future[Unit] = Future.successful(())

  override def getAndTransform[T](key: String, exp: Duration)(cb: (Option[T]) => T)(implicit codec: Codec[T]): Future[Option[T]] = Future.successful(None)

  override def get[T](key: String)(implicit codec: Codec[T]): Future[Option[T]] = Future.successful(None)

  override def delete(key: String): Future[Boolean] = Future.successful(false)

  override def compareAndSet[T](key: String, expecting: Option[T], newValue: T, exp: Duration)(implicit codec: Codec[T]): Future[Boolean] = Future.successful(false)

  override def close(): Unit = Unit

  override def transformAndGet[T](key: String, exp: Duration)(cb: (Option[T]) => T)(implicit codec: Codec[T]): Future[T] = Future.successful(cb(None))
}
