package com.m3.octoparts.cache

import com.google.common.cache.CacheBuilder
import com.m3.octoparts.cache.dummy.DummyRawCache
import com.m3.octoparts.cache.memcached.InMemoryRawCache
import org.scalatest.{ FunSpec, Matchers }
import play.api.Logger

import scala.concurrent.Await
import scala.concurrent.duration._

class MemoryBufferingRawCacheSpec extends FunSpec with Matchers {

  import shade.memcached.Codec.StringBinaryCodec

  it("should store data for a while") {
    val mbrc = new StatsRecordingMemoryBufferingRawCache(DummyRawCache, 200.milliseconds)

    Await.result(mbrc.set("ABC", "DEF", 1.hour), 10.millis)

    val result1 = Await.result(mbrc.get[String]("ABC"), 10.millis)
    result1 should be(Some("DEF"))
    mbrc.stats.hitCount() should be(1L)
    mbrc.stats.missCount() should be(0L)

    // wait for eviction delay
    Thread.sleep(mbrc.localCacheDuration.toMillis + 10L)

    val result2 = Await.result(mbrc.get[String]("ABC"), 10.millis)
    result2 should be(None)
    mbrc.stats.hitCount() should be(1L)
    mbrc.stats.missCount() should be(1L)
  }

  it("should automatically renew expired entries") {
    import scala.concurrent.ExecutionContext.Implicits.global
    val mbrc = new StatsRecordingMemoryBufferingRawCache(new InMemoryRawCache(), 200.milliseconds)
    Await.result(mbrc.set("ABC", "DEF", 1.hour), 10.millis)
    // reset lastStore
    mbrc.storeInMemoryCache("GGG", "")

    // wait for eviction delay
    Thread.sleep(mbrc.localCacheDuration.toMillis + 10L)

    // first try yields a miss, although the InMemory cache helps
    val result1 = Await.result(mbrc.get[String]("ABC"), 10.millis)
    result1 should be(Some("DEF"))
    mbrc.stats.hitCount() should be(0L)
    mbrc.stats.missCount() should be(1L)

    // wait for the local store to happen
    while (mbrc.lastStore.contains("GGG")) {
      Thread.sleep(5L)
    }

    // next try yields a hit
    val result2 = Await.result(mbrc.get[String]("ABC"), 10.millis)
    result2 should be(Some("DEF"))
    mbrc.stats.hitCount() should be(1L)
    mbrc.stats.missCount() should be(1L)
  }

  it("should discard entries when the expiry time is updated to a value that is too short") {

    val mbrc = new StatsRecordingMemoryBufferingRawCache(DummyRawCache, 30.minutes)

    Await.result(mbrc.set("ABC", "DEF", 1.hour), 10.millis)
    Await.result(mbrc.get[String]("ABC"), 10.millis) should be(Some("DEF"))

    Await.result(mbrc.set("ABC", "GHI", 25.minutes), 10.millis)
    Await.result(mbrc.get[String]("ABC"), 10.millis) should be(None)
  }
}

private class StatsRecordingMemoryBufferingRawCache(networkCache: RawCache, val localCacheDuration: Duration) extends MemoryBufferingRawCache(networkCache, localCacheDuration) {

  val cacheMap = memoryCache.asMap()

  @volatile var lastStore: Option[String] = None

  // init the logger. this takes a while on my system and can mess up this timing-sensitive test
  Logger.logger.getName

  override def configureMemoryCache(builder: CacheBuilder[Object, Object]) = super.configureMemoryCache(builder).recordStats()

  override def storeInMemoryCache(key: String, value: Any): Unit = {
    super.storeInMemoryCache(key, value)
    lastStore = Some(key)
  }

  def stats = memoryCache.stats()
}
