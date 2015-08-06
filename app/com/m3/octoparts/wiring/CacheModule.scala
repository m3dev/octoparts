package com.m3.octoparts.wiring

import java.util.concurrent.{ TimeUnit, ThreadPoolExecutor, ArrayBlockingQueue, BlockingQueue }

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.m3.octoparts.cache.dummy.{ DummyCacheOps, DummyCache, DummyRawCache, DummyLatestVersionCache }
import com.m3.octoparts.cache.key.MemcachedKeyGenerator
import com.m3.octoparts.cache.memcached.{ MemcachedCacheOps, MemcachedCache, InMemoryRawCache, MemcachedRawCache }
import com.m3.octoparts.cache.versioning.InMemoryLatestVersionCache
import com.m3.octoparts.cache.{ LoggingRawCache, RawCache }
import play.api.Play
import shade.memcached.{ AuthConfiguration, Memcached, Protocol, Configuration => ShadeConfig }

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import com.softwaremill.macwire._

trait CacheModule extends UtilsModule {

  private lazy val cacheExecutor: ExecutionContext = {
    val config = Play.configuration.underlying

    val namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("cache-%d").build()
    val poolSize = config.getInt("caching.pool.size")
    val queueSize = config.getInt("caching.queue.size")
    val queue: BlockingQueue[Runnable] = new ArrayBlockingQueue[Runnable](queueSize)

    ExecutionContext.fromExecutor(
      new ThreadPoolExecutor(0, poolSize, 1L, TimeUnit.MINUTES, queue, namedThreadFactory)
    )
  }

  lazy val latestVersionCache = {
    if (cachingDisabled) {
      DummyLatestVersionCache
    } else {
      val maxInMemoryLVCKeys = playConfig.getLong("caching.versionCachingSize").getOrElse(100000L)
      new InMemoryLatestVersionCache(maxInMemoryLVCKeys)
    }
  }

  lazy val rawCache = if (cachingDisabled) {
    DummyRawCache
  } else {
    val cache = if (useInMemoryCache) {
      new InMemoryRawCache()(cacheExecutor, zipkinService)
    } else {
      val hostPort = typesafeConfig.getString("memcached.host")
      val timeout = typesafeConfig.getInt("memcached.timeout").millis
      // should be one of "Text" or "Binary"
      val protocol = typesafeConfig.getString("memcached.protocol")
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
    sys.addShutdownHook {
      cache.close()
    }
    cache
  }

  lazy val memcachedKeyGenerator = MemcachedKeyGenerator

  lazy val cache = {
    if (cachingDisabled) {
      DummyCache
    } else {
      wire[MemcachedCache]
    }
  }

  lazy val cacheOps = {
    if (cachingDisabled) {
      DummyCacheOps
    } else {
      wire[MemcachedCacheOps]
    }
  }

  lazy val cachingDisabled = playConfig.getBoolean("caching.disabled").getOrElse(false)

  lazy val useInMemoryCache = playConfig.getBoolean("caching.inmemory").getOrElse(false)

}