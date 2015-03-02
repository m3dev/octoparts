package com.m3.octoparts.cache

import java.util.concurrent.TimeUnit

import com.beachape.logging.LTSVLogger
import com.google.common.cache.{ CacheBuilder, Cache => GuavaCache }
import com.twitter.zipkin.gen.Span
import shade.memcached.Codec

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

/**
 * Intended as a short buffering component, in front of a network cache, to improve response time and decrease network load.
 * As such, it won't try to buffer cache entries which lifetime is short enough
 * @param networkCache the cache to be buffered
 * @param localCacheDuration should be tuned to compromise between :
 *                           - performance (long buffering)
 *                           - receptiveness to external changes (in case the network cache is shared)
 *
 */
class MemoryBufferingRawCache(networkCache: RawCache, localCacheDuration: Duration) extends RawCache {

  protected val memoryCache: GuavaCache[String, Object] = configureMemoryCache(CacheBuilder.newBuilder()).build[String, Object]()

  // exposed for testing
  protected def configureMemoryCache(builder: CacheBuilder[Object, Object]): CacheBuilder[Object, Object] = {
    builder.expireAfterWrite(localCacheDuration.toMillis, TimeUnit.MILLISECONDS)
  }

  def set[T](key: String, value: T, exp: Duration)(implicit codec: Codec[T], parentSpan: Span): Future[Unit] = {
    if (exp >= localCacheDuration) {
      storeInMemoryCache(key, value)
    } else {
      // guarantees that the buffer won't hold a reference to an obsolete value if this operation was a mere update to expiry time
      removeFromMemoryCache(key)
    }
    networkCache.set(key, value, exp)
  }

  def get[T](key: String)(implicit codec: Codec[T], parentSpan: Span): Future[Option[T]] = {
    val local = Option(memoryCache.getIfPresent(key))
    local match {
      case Some(value) => Future.successful(Some(value.asInstanceOf[T]))
      case _ =>
        LTSVLogger.trace("Key missing in local cache" -> key)
        val cachePoll = networkCache.get(key)

        cachePoll.onSuccess {
          // this may cause the value to be in the local cache for a while after remote cache expiry.
          // However, the intended use case is remoteCacheDuration >> localCacheDuration, in which case this is not a big issue.
          case Some(value) => storeInMemoryCache(key, value)
        }(ExecutionContext.global)
        cachePoll
    }
  }

  def close(): Unit = {
    memoryCache.invalidateAll()
    networkCache.close()
  }

  protected def storeInMemoryCache(key: String, value: Any): Unit = value match {
    case obj: Object => {
      memoryCache.put(key, obj)
      LTSVLogger.trace("Key set in local cache" -> key, "to" -> obj.toString, "for" -> localCacheDuration.toString)
    }
    case _ =>
  }

  protected def removeFromMemoryCache(key: String) = {
    memoryCache.invalidate(key)
    LTSVLogger.trace("Key removed from local cache" -> key)
  }
}