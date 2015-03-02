package com.m3.octoparts.cache.memcached

import java.util.concurrent.Executors

import com.beachape.zipkin.services.NoopZipkinService
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.m3.octoparts.cache.RawCache
import com.m3.octoparts.cache.key.{ CacheKey, MemcachedKeyGenerator, VersionCacheKey }
import com.twitter.zipkin.gen.Span
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ FunSpec, Matchers }
import shade.memcached.Codec
import shade.memcached.Codec.IntBinaryCodec

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

class MemcachedCacheSpec extends FunSpec with Matchers with ScalaFutures {

  implicit lazy val cacheExecutor = {
    val namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("cache-%d").build()
    ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor(namedThreadFactory))
  }
  implicit val emptySpan = new Span()
  implicit val zipkinService = NoopZipkinService

  describe("error handling") {

    object MessedUpAdapter extends RawCache {
      override def set[T](key: String, value: T, exp: Duration)(implicit codec: Codec[T], parentSpan: Span): Future[Unit] = ???
      override def get[T](key: String)(implicit codec: Codec[T], parentSpan: Span): Future[Option[T]] = ???
      override def close(): Unit = ???
    }

    val cache = new MemcachedCache(MessedUpAdapter, MemcachedKeyGenerator)

    it("should always throw in a Future") {
      val fPut = cache.put(VersionCacheKey("hi"), "hello", None)
      val fGet = cache.get[String](VersionCacheKey("har"))
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

    class MockMemcached extends RawCache {
      override def get[T](key: String)(implicit codec: Codec[T], parentSpan: Span): Future[Option[T]] = ???
      override def close(): Unit = ???

      var insertedWithTtl: Option[Duration] = None
      override def set[T](key: String, value: T, exp: Duration)(implicit codec: Codec[T], parentSpan: Span): Future[Unit] = {
        insertedWithTtl = Some(exp)
        Future.successful(())
      }
    }

    it("should skip the cache put when the TTL is exactly zero") {
      val cache = new MockMemcached
      val subject = new MemcachedCache(cache, MockKeyGen)
      whenReady(subject.put(SimpleCacheKey("foo"), 123, Some(Duration.Zero))) { _ =>
        cache.insertedWithTtl should be(None)
      }
    }

    it("should skip the cache put when the TTL is less than one second") {
      val cache = new MockMemcached
      val subject = new MemcachedCache(cache, MockKeyGen)
      whenReady(subject.put(SimpleCacheKey("foo"), 123, Some(900.millis))) { _ =>
        cache.insertedWithTtl should be(None)
      }
    }

    it("should perform the cache put when the TTL is exactly one second") {
      val cache = new MockMemcached
      val subject = new MemcachedCache(cache, MockKeyGen)
      whenReady(subject.put(SimpleCacheKey("foo"), 123, Some(1.second))) { _ =>
        cache.insertedWithTtl should be(Some(1.second))
      }
    }

    it("should perform the cache put when the TTL is None, i.e. infinite") {
      val cache = new MockMemcached
      val subject = new MemcachedCache(cache, MockKeyGen)
      whenReady(subject.put(SimpleCacheKey("foo"), 123, None)) { _ =>
        cache.insertedWithTtl should be(Some(30.days))
      }
    }
  }
}
