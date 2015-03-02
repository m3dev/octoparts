package com.m3.octoparts.cache.memcached

import com.beachape.zipkin.services.ZipkinServiceLike
import com.m3.octoparts.cache.{ Cache, CacheException, RawCache }
import com.m3.octoparts.cache.key._
import com.beachape.logging.LTSVLogger
import com.twitter.zipkin.gen.Span
import shade.memcached.Codec

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal

/**
 * A [[Cache]] implementation that performs the following Memcached-specific processing:
 * - converts the cache key to a String that can be used as a Memcached key
 * - skips cache PUTs with TTLs < 1 second
 * - adds error handling for any exceptions thrown synchronously by Spymemcached/Shade
 *
 * @param underlying the underlying raw cache
 * @param keyGen the key generator
 */
class MemcachedCache(underlying: RawCache, keyGen: MemcachedKeyGenerator)(implicit executionContext: ExecutionContext, zipkinService: ZipkinServiceLike)
    extends Cache {

  import com.beachape.zipkin.FutureEnrichment._

  /**
   * This value is arbitrarily chosen.
   * We could also use Duration.Inf, which Shade would convert to 365 days,
   * or Duration.Zero, which Memcached would treat as infinite.
   */
  private val VERY_LONG_TTL = 30.days

  private def serializeKey(key: CacheKey) = keyGen.toMemcachedKey(key)

  def get[T](key: CacheKey)(implicit codec: Codec[T], parentSpan: Span): Future[Option[T]] = {
    try {
      underlying.get[T](serializeKey(key)).recoverWith {
        case NonFatal(err) => throw new CacheException(key, err)
      }.trace(s"memcached-get-$key")
    } catch {
      case NonFatal(e) => Future.failed(e)
    }
  }

  def put[T](key: CacheKey, v: T, ttl: Option[Duration])(implicit codec: Codec[T], parentSpan: Span): Future[Unit] = {
    try {
      ttl match {
        case Some(duration) if duration < 1.second =>
          /*
           * If the TTL is less than one second, we should not perform a cache insert, because:
           *  - there is no point, as the element has already expired, or will do very soon
           *  - The TTL will get rounded down to 0, which Memcached treats as meaning "infinite". This is the exact opposite to what we want.
           */
          LTSVLogger.debug("message" -> "Skipping cache PUT because ttl is less than 1 second", "key" -> key.toString, "ttl" -> duration.toString)
          Future.successful(())
        case _ =>
          underlying.set[T](serializeKey(key), v, ttl.getOrElse(VERY_LONG_TTL)).recoverWith {
            case NonFatal(err) => throw new CacheException(key, err)
          }.trace(s"memcached-set-$key")
      }
    } catch {
      case NonFatal(e) => Future.failed(e)
    }
  }

}
