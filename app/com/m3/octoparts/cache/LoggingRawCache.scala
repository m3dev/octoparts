package com.m3.octoparts.cache

import com.m3.octoparts.logging.{ LTSVLogWriter, LogUtil }
import play.api.Logger
import shade.memcached.Codec
import skinny.util.LTSV

import scala.concurrent.duration.Duration
import scala.concurrent.{ ExecutionContext, Future }

/**
 * A decorator to add debug logging to a [[RawCache]]
 */
class LoggingRawCache(delegate: RawCache)(implicit executionContext: ExecutionContext) extends RawCache with LogUtil {

  def get[T](key: String)(implicit codec: Codec[T]): Future[Option[T]] = {
    val f = delegate.get(key)(codec)
    f.onSuccess {
      case mbVal => LTSVLogWriter.debug("Memcached" -> "get", "key" -> key, "is" -> truncateValue(mbVal))
    }
    f
  }

  def set[T](key: String, value: T, exp: Duration)(implicit codec: Codec[T]): Future[Unit] = {
    val f = delegate.set(key, value, exp)(codec)
    f.onSuccess {
      case done => LTSVLogWriter.debug("Memcached" -> "set", "key" -> key, "value" -> truncateValue(value), "duration" -> exp.toString)
    }
    f
  }

  def close(): Unit = delegate.close()

}
