package com.m3.octoparts.cache

import java.util.concurrent.{ TimeUnit, _ }

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.m3.octoparts.cache.memcached._
import com.m3.octoparts.cache.dummy.{ DummyCache, DummyRawCache, DummyCacheOps, DummyLatestVersionCache }
import com.m3.octoparts.cache.key.MemcachedKeyGenerator
import com.m3.octoparts.cache.versioning.{ InMemoryLatestVersionCache, LatestVersionCache }
import play.api.Configuration
import scaldi.{ Condition, Module }
import shade.memcached.{ AuthConfiguration, Memcached, Protocol, Configuration => ShadeConfig }

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class CacheModule extends Module {

  private lazy val cacheExecutor = {
    val config = inject[Configuration].underlying

    val namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("cache-%d").build()
    val poolSize = config.getInt("caching.pool.size")
    val queueSize = config.getInt("caching.queue.size")
    val queue: BlockingQueue[Runnable] = new ArrayBlockingQueue[Runnable](queueSize)

    ExecutionContext.fromExecutor(
      new ThreadPoolExecutor(0, poolSize, 1L, TimeUnit.MINUTES, queue, namedThreadFactory))
  }

  private def buildMemcachedRawCache(): RawCache = {
    val playConfig = inject[Configuration]
    val tsConfig = playConfig.underlying

    val hostPort = tsConfig.getString("memcached.host")
    val timeout = tsConfig.getInt("memcached.timeout").millis
    // should be one of "Text" or "Binary"
    val protocol = tsConfig.getString("memcached.protocol")
    val auth = for {
      user <- playConfig.getString("memcached.user")
      password <- playConfig.getString("memcached.password")
    } yield AuthConfiguration(user, password)

    val shade = Memcached(ShadeConfig(
      addresses = hostPort,
      operationTimeout = timeout,
      protocol = Protocol.withName(protocol),
      authentication = auth
    ), cacheExecutor)

    new LoggingRawCache(new MemcachedRawCache(shade))(cacheExecutor)
  }

  when(cachingEnabled) {
    bind[LatestVersionCache] to new InMemoryLatestVersionCache
    when(useInMemoryCache) {
      bind[RawCache] to new InMemoryRawCache()(cacheExecutor) destroyWith (_.close())
    }
    when(useMemcached) {
      bind[RawCache] to buildMemcachedRawCache() destroyWith (_.close())
    }
    bind[MemcachedKeyGenerator] to MemcachedKeyGenerator
    bind[Cache] to injected[MemcachedCache]
    bind[CacheOps] to injected[MemcachedCacheOps]
  }

  when(cachingDisabled) {
    bind[LatestVersionCache] to DummyLatestVersionCache
    bind[RawCache] to DummyRawCache
    bind[Cache] to DummyCache
    bind[CacheOps] to DummyCacheOps
  }

  def cachingDisabled = {
    val isDisabled = inject[Configuration].getBoolean("caching.disabled").getOrElse(false)
    Condition(isDisabled)
  }

  def cachingEnabled = !cachingDisabled

  def useInMemoryCache = {
    val flag = inject[Configuration].getBoolean("caching.inmemory").getOrElse(false)
    Condition(flag)
  }

  def useMemcached = !useInMemoryCache

}
