package com.m3.octoparts.repository

import com.m3.octoparts.cache.key.MemcachedKeyGenerator
import com.m3.octoparts.cache.memcached._
import com.m3.octoparts.http.HttpClientPool
import play.api.Configuration
import com.m3.octoparts.cache.{RawCache, Cache}
import scaldi.Module

import scala.concurrent.duration._

class RepositoriesModule extends Module {

  bind[MutableConfigsRepository] to {
    import scala.concurrent.ExecutionContext.Implicits.global

    val localBuffer = inject[Configuration].getInt("memcached.configLocalBuffer")

    val cache = localBuffer match {
      case Some(localBufferDuration) => {
        val networkCache = inject[RawCache]
        val bufferingCache = new MemoryBufferingRawCache(networkCache, localBufferDuration.millis)
        new MemcachedCache(bufferingCache, MemcachedKeyGenerator)
      }
      case None => inject[Cache]
    }

    new MutableCachingRepository(
      DBConfigsRepository,
      cache,
      inject[HttpClientPool]
    )
  }

}
