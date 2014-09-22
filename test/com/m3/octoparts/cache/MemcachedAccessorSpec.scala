package com.m3.octoparts.cache

import java.util.concurrent.Executors

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.m3.octoparts.cache.client.MemcachedAccessor
import com.m3.octoparts.cache.key.{ CacheKey, MemcachedKeyGenerator, VersionCacheKey }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ FunSpec, Matchers }
import shade.memcached.Codec.IntBinaryCodec
import shade.memcached.{ Codec, Memcached }

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

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

  describe("near-zero TTLs") {

    case class SimpleCacheKey(key: String) extends CacheKey

    object MockKeyGen extends MemcachedKeyGenerator {
      def toMemcachedKey(rawKey: CacheKey) = "key"
    }

    class MockMemcached extends Memcached {
      override def add[T](key: String, value: T, exp: Duration)(implicit codec: Codec[T]): Future[Boolean] = ???
      override def getAndTransform[T](key: String, exp: Duration)(cb: (Option[T]) => T)(implicit codec: Codec[T]): Future[Option[T]] = ???
      override def get[T](key: String)(implicit codec: Codec[T]): Future[Option[T]] = ???
      override def delete(key: String): Future[Boolean] = ???
      override def compareAndSet[T](key: String, expecting: Option[T], newValue: T, exp: Duration)(implicit codec: Codec[T]): Future[Boolean] = ???
      override def close(): Unit = ???
      override def transformAndGet[T](key: String, exp: Duration)(cb: (Option[T]) => T)(implicit codec: Codec[T]): Future[T] = ???

      var insertedWithTtl: Option[Duration] = None
      override def set[T](key: String, value: T, exp: Duration)(implicit codec: Codec[T]): Future[Unit] = {
        insertedWithTtl = Some(exp)
        Future.successful(())
      }
    }

    it("should skip the cache put when the TTL is exactly zero") {
      val cache = new MockMemcached
      val subject = new MemcachedAccessor(cache, MockKeyGen)
      whenReady(subject.doPut(SimpleCacheKey("foo"), 123, Some(Duration.Zero))) { _ =>
        cache.insertedWithTtl should be(None)
      }
    }

    it("should skip the cache put when the TTL is less than one second") {
      val cache = new MockMemcached
      val subject = new MemcachedAccessor(cache, MockKeyGen)
      whenReady(subject.doPut(SimpleCacheKey("foo"), 123, Some(900.millis))) { _ =>
        cache.insertedWithTtl should be(None)
      }
    }

    it("should perform the cache put when the TTL is exactly one second") {
      val cache = new MockMemcached
      val subject = new MemcachedAccessor(cache, MockKeyGen)
      whenReady(subject.doPut(SimpleCacheKey("foo"), 123, Some(1.second))) { _ =>
        cache.insertedWithTtl should be(Some(1.second))
      }
    }

    it("should perform the cache put when the TTL is None, i.e. infinite") {
      val cache = new MockMemcached
      val subject = new MemcachedAccessor(cache, MockKeyGen)
      whenReady(subject.doPut(SimpleCacheKey("foo"), 123, None)) { _ =>
        cache.insertedWithTtl should be(Some(30.days))
      }
    }
  }
}
