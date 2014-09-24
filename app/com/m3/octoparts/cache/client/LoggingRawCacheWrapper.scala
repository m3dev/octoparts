package com.m3.octoparts.cache.client

import com.m3.octoparts.logging.LogUtil
import play.api.Logger
import shade.memcached.Codec
import skinny.util.LTSV

import scala.concurrent.duration.Duration
import scala.concurrent.{ ExecutionContext, Future }

class LoggingRawCacheWrapper(delegate: RawCache)(implicit executionContext: ExecutionContext) extends RawCache with LogUtil {

  def get[T](key: String)(implicit codec: Codec[T]): Future[Option[T]] = {
    val f = delegate.get(key)(codec)
    f.onSuccess {
      case mbVal => Logger.debug(LTSV.dump("Memcached" -> "get", "key" -> key, "is" -> truncateValue(mbVal)))
    }
    f
  }

  def set[T](key: String, value: T, exp: Duration)(implicit codec: Codec[T]): Future[Unit] = {
    val f = delegate.set(key, value, exp)(codec)
    f.onSuccess {
      case done => Logger.debug(LTSV.dump("Memcached" -> "set", "key" -> key, "value" -> truncateValue(value), "duration" -> exp.toString))
    }
    f
  }

  def close(): Unit = delegate.close()

}
