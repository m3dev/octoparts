package com.m3.octoparts.cache.memcached

import java.util.concurrent.TimeUnit

import com.google.common.cache.{CacheBuilder, Cache => GuavaCache}
import com.m3.octoparts.cache.RawCache
import play.api.Logger
import shade.memcached.Codec
import skinny.util.LTSV

import scala.concurrent.Future
import scala.concurrent.duration._

class MemoryBufferingRawCache(networkCache: RawCache, localCacheDuration: Duration) extends RawCache {

  private val memoryCache: GuavaCache[String, Object] =
    CacheBuilder.newBuilder()
      .expireAfterWrite(localCacheDuration.toMillis, TimeUnit.MILLISECONDS)
      .build[String, Object]()

  override def set[T](key: String, value: T, exp: Duration)(implicit codec: Codec[T]): Future[Unit] = {
    if (exp >= localCacheDuration) {
      memoryCache.put(key, value.asInstanceOf[Object])
      if (Logger.isTraceEnabled) {
        Logger.trace(LTSV.dump("Key set in local cache" -> key, "for" -> localCacheDuration.toString))
      }
    }
    networkCache.set(key, value, exp)
  }

  override def get[T](key: String)(implicit codec: Codec[T]): Future[Option[T]] = {
    val local = Option(memoryCache.getIfPresent(key))
    local match {
      case None =>
        if (Logger.isTraceEnabled) {
          Logger.trace(LTSV.dump("Key missing in local cache" -> key))
        }
        networkCache.get(key)
      case Some(obj) => Future.successful(Some(obj.asInstanceOf[T]))
    }
  }

  override def close(): Unit = {
    memoryCache.asMap().clear()
    networkCache.close()
  }

}