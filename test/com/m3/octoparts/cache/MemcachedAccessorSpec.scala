package com.m3.octoparts.cache

import java.util.concurrent.Executors

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.m3.octoparts.cache.client.MemcachedAccessor
import com.m3.octoparts.cache.key.{ VersionCacheKey, MemcachedKeyGenerator }
import org.scalatest.{ Matchers, FunSpec }
import org.scalatest.concurrent.ScalaFutures
import shade.memcached.{ Codec, Memcached }

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.Duration

class MemcachedAccessorSpec extends FunSpec with Matchers with ScalaFutures {

  implicit lazy val cacheExecutor = {
    val namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("cache-%d").build()
    ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor(namedThreadFactory))
  }

  describe("error handling") {

    object MessedUpAdapter extends Memcached {
      override def add[T](key: String, value: T, exp: Duration)(implicit codec: Codec[T]): Future[Boolean] = ???
      override def set[T](key: String, value: T, exp: Duration)(implicit codec: Codec[T]): Future[Unit] = ???
      override def getAndTransform[T](key: String, exp: Duration)(cb: (Option[T]) => T)(implicit codec: Codec[T]): Future[Option[T]] = ???
      override def get[T](key: String)(implicit codec: Codec[T]): Future[Option[T]] = ???
      override def delete(key: String): Future[Boolean] = ???
      override def compareAndSet[T](key: String, expecting: Option[T], newValue: T, exp: Duration)(implicit codec: Codec[T]): Future[Boolean] = ???
      override def close(): Unit = ???
      override def transformAndGet[T](key: String, exp: Duration)(cb: (Option[T]) => T)(implicit codec: Codec[T]): Future[T] = ???
    }

    val cacheAccessor = new MemcachedAccessor(MessedUpAdapter, MemcachedKeyGenerator)

    it("should always throw in a Future") {
      val fPut = cacheAccessor.doPut(VersionCacheKey("hi"), "hello", None)
      val fGet = cacheAccessor.doGet[String](VersionCacheKey("har"))
      whenReady(fPut.failed) { e =>
        e.getCause.getMessage should include("an implementation is missing")
      }
      whenReady(fGet.failed) { e =>
        e.getCause.getMessage should include("an implementation is missing")
      }
    }

  }
}
