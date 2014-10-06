package com.m3.octoparts.cache

import java.util.concurrent.TimeUnit

import com.google.common.cache.{ CacheBuilder, Cache => GuavaCache }
import play.api.Logger
import shade.memcached.Codec
import skinny.util.LTSV

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._

class MemoryBufferingRawCache(networkCache: RawCache, localCacheDuration: Duration) extends RawCache {

  private val memoryCache: GuavaCache[String, Object] =
    CacheBuilder.newBuilder()
      .expireAfterWrite(localCacheDuration.toMillis, TimeUnit.MILLISECONDS)
      .build[String, Object]()

  def set[T](key: String, value: T, exp: Duration)(implicit codec: Codec[T]): Future[Unit] = {
    if (exp >= localCacheDuration) {
      storeInMemoryCache(key, value)
    }
    networkCache.set(key, value, exp)
  }

  def get[T](key: String)(implicit codec: Codec[T]): Future[Option[T]] = {
    val local = Option(memoryCache.getIfPresent(key))
    local match {
      case Some(value) => Future.successful(Some(value.asInstanceOf[T]))
      case _ =>
        if (Logger.isTraceEnabled) {
          Logger.trace(LTSV.dump("Key missing in local cache" -> key))
        }
        val cachePoll = networkCache.get(key)
        cachePoll.onSuccess {
          case Some(value) => storeInMemoryCache(key, value)
        }(ExecutionContext.global)
        cachePoll
    }
  }

  def close(): Unit = {
    memoryCache.asMap().clear()
    networkCache.close()
  }

  private def storeInMemoryCache(key: String, value: Any): Unit = {
    value match {
      case obj: Object =>
        memoryCache.put(key, obj)
        if (Logger.isTraceEnabled) {
          Logger.trace(LTSV.dump("Key set in local cache" -> key, "to" -> obj.toString, "for" -> localCacheDuration.toString))
        }
      case _ =>
    }
  }
}