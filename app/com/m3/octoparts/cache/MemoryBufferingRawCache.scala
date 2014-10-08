package com.m3.octoparts.cache

import java.util.concurrent.TimeUnit

import com.beachape.logging.LTSVLogger
import com.google.common.cache.{ CacheBuilder, Cache => GuavaCache }
import shade.memcached.Codec

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

class MemoryBufferingRawCache(networkCache: RawCache, localCacheDuration: Duration) extends RawCache {

  protected val memoryCache: GuavaCache[String, Object] = configureMemoryCache(CacheBuilder.newBuilder()).build[String, Object]()

  // exposed for testing
  protected def configureMemoryCache(builder: CacheBuilder[Object, Object]): CacheBuilder[Object, Object] = {
    builder.expireAfterWrite(localCacheDuration.toMillis, TimeUnit.MILLISECONDS)
  }

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
        LTSVLogger.trace("Key missing in local cache" -> key)
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

  protected def storeInMemoryCache(key: String, value: Any): Unit = value match {
    case obj: Object => {
      memoryCache.put(key, obj)
      LTSVLogger.trace("Key set in local cache" -> key, "to" -> obj.toString, "for" -> localCacheDuration.toString)
    }
    case _ =>
  }
}