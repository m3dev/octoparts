package com.m3.octoparts.cache.client

import shade.inmemory.InMemoryCache
import shade.memcached.{ Codec, Memcached }

import scala.concurrent.duration.Duration
import scala.concurrent.{ ExecutionContext, Future }

/**
 * An in-memory cache that can be used as a drop-in replacement for Memcached
 */
class InMemoryCacheAdapter()(implicit executionContext: ExecutionContext) extends Memcached {
  private val impl = InMemoryCache(executionContext)

  override def add[T](key: String, value: T, exp: Duration)(implicit codec: Codec[T]): Future[Boolean] = Future.successful(impl.add(key, value, exp))

  override def close(): Unit = impl.close()

  override def getAndTransform[T](key: String, exp: Duration)(cb: (Option[T]) => T)(implicit codec: Codec[T]): Future[Option[T]] =
    Future.successful(impl.getAndTransform(key, exp)(cb))

  override def transformAndGet[T](key: String, exp: Duration)(cb: (Option[T]) => T)(implicit codec: Codec[T]): Future[T] =
    Future.successful(impl.transformAndGet(key, exp)(cb))

  override def compareAndSet[T](key: String, expecting: Option[T], newValue: T, exp: Duration)(implicit codec: Codec[T]): Future[Boolean] =
    Future.successful(impl.compareAndSet(key, expecting, newValue, exp))

  override def get[T](key: String)(implicit codec: Codec[T]): Future[Option[T]] = Future {
    impl.get(key)
  }

  override def delete(key: String): Future[Boolean] = Future {
    impl.delete(key)
  }

  override def set[T](key: String, value: T, exp: Duration)(implicit codec: Codec[T]): Future[Unit] = Future {
    impl.set(key, value, exp)
  }
}
