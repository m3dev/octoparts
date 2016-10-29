package com.m3.octoparts.wiring

import com.m3.octoparts.cache.MemoryBufferingRawCache
import com.m3.octoparts.cache.memcached.MemcachedCache
import com.m3.octoparts.repository.{ DBConfigsRepository, MutableCachingRepository }
import scala.concurrent.duration._

trait RepositoriesModule
    extends CacheModule
    with HttpClientPoolModule
    with ExecutionContextsModule {

  import com.softwaremill.macwire._

  private implicit lazy val ec = dbFetchExecutionContext

  lazy val configsRepository = {
    val localBuffer = configuration.getInt("memcached.configLocalBuffer")

    val mutableRepoCache = localBuffer match {
      case Some(localBufferDuration) if localBufferDuration > 0 => {
        val networkCache = rawCache
        val bufferingCache = new MemoryBufferingRawCache(networkCache, localBufferDuration.millis)
        new MemcachedCache(bufferingCache, memcachedKeyGenerator)
      }
      case _ => cache
    }

    new MutableCachingRepository(
      wire[DBConfigsRepository],
      mutableRepoCache,
      httpClientPool
    )
  }

}
