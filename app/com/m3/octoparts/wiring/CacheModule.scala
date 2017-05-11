package com.m3.octoparts.wiring

import java.util.concurrent.{ ArrayBlockingQueue, BlockingQueue, ThreadPoolExecutor, TimeUnit }

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.m3.octoparts.cache.dummy.{ DummyCache, DummyCacheOps, DummyLatestVersionCache, DummyRawCache }
import com.m3.octoparts.cache.key.MemcachedKeyGenerator
import com.m3.octoparts.cache.memcached.{ InMemoryRawCache, MemcachedCache, MemcachedCacheOps, MemcachedRawCache }
import com.m3.octoparts.cache.versioning.{ InMemoryLatestVersionCache, LatestVersionCache }
import com.m3.octoparts.cache.{ LoggingRawCache, RawCache }
import shade.memcached.{ AuthConfiguration, FailureMode, Memcached, Protocol, Configuration => ShadeConfig }

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import com.softwaremill.macwire._

trait CacheModule extends UtilsModule {

  private implicit lazy val cacheExecutor: ExecutionContext = {

    val namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("cache-%d").build()
    val poolSize = configuration.get[Int]("caching.pool.size")
    val queueSize = configuration.get[Int]("caching.queue.size")
    val queue: BlockingQueue[Runnable] = new ArrayBlockingQueue[Runnable](queueSize)

    ExecutionContext.fromExecutor(
      new ThreadPoolExecutor(0, poolSize, 1L, TimeUnit.MINUTES, queue, namedThreadFactory)
    )
  }

  lazy val latestVersionCache: LatestVersionCache = {
    if (cachingDisabled) {
      DummyLatestVersionCache
    } else {
      val maxInMemoryLVCKeys = configuration.getOptional[Long]("caching.versionCachingSize").getOrElse(100000L)
      new InMemoryLatestVersionCache(maxInMemoryLVCKeys)
    }
  }

  lazy val rawCache: RawCache = if (cachingDisabled) {
    DummyRawCache
  } else {
    val cache = if (useInMemoryCache) {
      new InMemoryRawCache(zipkinService)(cacheExecutor)
    } else {
      val hostPort = configuration.get[String]("memcached.host")
      val timeout = configuration.get[Int]("memcached.timeout").millis
      // should be one of "Text" or "Binary"
      val protocol = configuration.get[String]("memcached.protocol")
      val auth = for {
        user <- configuration.getOptional[String]("memcached.user")
        password <- configuration.getOptional[String]("memcached.password")
      } yield AuthConfiguration(user, password)

      val shade = Memcached(
        ShadeConfig(
          addresses = hostPort,
          operationTimeout = timeout,
          protocol = Protocol.withName(protocol),
          authentication = auth,
          failureMode = FailureMode.Redistribute
        )
      )(cacheExecutor)

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

  lazy val cachingDisabled = configuration.getOptional[Boolean]("caching.disabled").getOrElse(false)

  lazy val useInMemoryCache = configuration.getOptional[Boolean]("caching.inmemory").getOrElse(false)

}