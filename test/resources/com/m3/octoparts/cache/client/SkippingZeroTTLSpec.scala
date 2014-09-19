package com.m3.octoparts.cache.client

import com.m3.octoparts.cache.key.CacheKey
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ FlatSpec, Matchers }
import shade.memcached.Codec
import shade.memcached.Codec.IntBinaryCodec

import scala.concurrent.Future
import scala.concurrent.duration._

class SkippingZeroTTLSpec extends FlatSpec with Matchers with ScalaFutures {

  case class SimpleCacheKey(key: String) extends CacheKey

  class MockCacheAccessor extends CacheAccessor {
    def doGet[T](key: CacheKey)(implicit codec: Codec[T]) = ???

    var inserted: Option[(CacheKey, Option[Duration])] = None

    def doPut[T](key: CacheKey, v: T, ttl: Option[Duration])(implicit codec: Codec[T]) = {
      inserted = Some((key, ttl))
      Future.successful(())
    }
  }

  it should "skip the cache put when the TTL is exactly zero" in {
    val subject = new MockCacheAccessor with SkippingZeroTTL
    whenReady(subject.doPut(SimpleCacheKey("foo"), 123, Some(Duration.Zero))) { _ =>
      subject.inserted should be(None)
    }
  }

  it should "skip the cache put when the TTL is less than one second" in {
    val subject = new MockCacheAccessor with SkippingZeroTTL
    whenReady(subject.doPut(SimpleCacheKey("foo"), 123, Some(900.millis))) { _ =>
      subject.inserted should be(None)
    }
  }

  it should "perform the cache put when the TTL is exactly one second" in {
    val subject = new MockCacheAccessor with SkippingZeroTTL
    whenReady(subject.doPut(SimpleCacheKey("foo"), 123, Some(1.second))) { _ =>
      subject.inserted should be(Some(SimpleCacheKey("foo"), Some(1.second)))
    }
  }

  it should "perform the cache put when the TTL is None, i.e. infinite" in {
    val subject = new MockCacheAccessor with SkippingZeroTTL
    whenReady(subject.doPut(SimpleCacheKey("foo"), 123, None)) { _ =>
      subject.inserted should be(Some(SimpleCacheKey("foo"), None))
    }
  }

}
