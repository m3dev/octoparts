package com.m3.octoparts.cache

import com.m3.octoparts.logging.LogUtil
import com.beachape.logging.LTSVLogger
import shade.memcached.Codec

import scala.concurrent.duration.Duration
import scala.concurrent.{ ExecutionContext, Future }

/**
 * A decorator to add debug logging to a [[RawCache]]
 */
class LoggingRawCache(delegate: RawCache)(implicit executionContext: ExecutionContext) extends RawCache with LogUtil {

  def get[T](key: String)(implicit codec: Codec[T]): Future[Option[T]] = {
    val f = delegate.get(key)(codec)
    f.onSuccess {
      case mbVal => LTSVLogger.debug("Memcached" -> "get", "key" -> key, "is" -> truncateValue(mbVal))
    }
    f
  }

  def set[T](key: String, value: T, exp: Duration)(implicit codec: Codec[T]): Future[Unit] = {
    val f = delegate.set(key, value, exp)(codec)
    f.onSuccess {
      case done => LTSVLogger.debug("Memcached" -> "set", "key" -> key, "value" -> truncateValue(value), "duration" -> exp.toString)
    }
    f
  }

  def close(): Unit = delegate.close()

}
