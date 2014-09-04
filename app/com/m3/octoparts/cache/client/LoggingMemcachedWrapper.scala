package com.m3.octoparts.cache.client

import com.m3.octoparts.logging.LogUtil
import play.api.Logger
import shade.memcached.{ Codec, Memcached }
import skinny.util.LTSV

import scala.concurrent.duration.Duration
import scala.concurrent.{ ExecutionContext, Future }

class LoggingMemcachedWrapper(delegate: Memcached)(implicit executionContext: ExecutionContext) extends Memcached with LogUtil {

  override def add[T](key: String, value: T, exp: Duration)(implicit codec: Codec[T]): Future[Boolean] = {
    val f = delegate.add(key, value, exp)(codec)
    f.onSuccess {
      case true => Logger.debug(LTSV.dump("Memcached" -> "add", "key" -> key, "value" -> String.valueOf(value), "duration" -> exp.toString))
      case false => Logger.debug(s"Did not add $key as it is already there")
    }
    f
  }

  override def set[T](key: String, value: T, exp: Duration)(implicit codec: Codec[T]): Future[Unit] = {
    val f = delegate.set(key, value, exp)(codec)
    f.onSuccess {
      case done => Logger.debug(LTSV.dump("Memcached" -> "set", "key" -> key, "value" -> truncateValue(value), "duration" -> exp.toString))
    }
    f
  }

  override def getAndTransform[T](key: String, exp: Duration)(cb: (Option[T]) => T)(implicit codec: Codec[T]): Future[Option[T]] = {
    val f = delegate.getAndTransform(key, exp)(cb)(codec)
    f.onSuccess {
      case mbVal => Logger.debug(LTSV.dump("Memcached" -> "getAndTransform", "key" -> key, "duration" -> exp.toString, "was" -> truncateValue(mbVal)))
    }
    f
  }

  override def get[T](key: String)(implicit codec: Codec[T]): Future[Option[T]] = {
    val f = delegate.get(key)(codec)
    f.onSuccess {
      case mbVal => Logger.debug(LTSV.dump("Memcached" -> "get", "key" -> key, "is" -> truncateValue(mbVal)))
    }
    f
  }

  override def delete(key: String): Future[Boolean] = {
    val f = delegate.delete(key)
    f.onSuccess {
      case deleted => Logger.debug(LTSV.dump("Memcached" -> "delete", "key" -> key, "actually deleted" -> (if (deleted) "yes" else "no")))
    }
    f
  }

  override def compareAndSet[T](key: String, expecting: Option[T], newValue: T, exp: Duration)(implicit codec: Codec[T]): Future[Boolean] = {
    val f = delegate.compareAndSet(key, expecting, newValue, exp)(codec)
    f.onSuccess {
      case matched => Logger.debug(LTSV.dump("Memcached" -> "compareAndSet",
        "key" -> key,
        "expected" -> expecting.toString,
        "newValue" -> truncateValue(newValue),
        "duration" -> exp.toString,
        "matched" -> matched.toString))
    }
    f
  }

  override def close(): Unit = delegate.close()

  override def transformAndGet[T](key: String, exp: Duration)(cb: (Option[T]) => T)(implicit codec: Codec[T]): Future[T] = {
    val f = delegate.transformAndGet(key, exp)(cb)(codec)
    f.onSuccess {
      case value => Logger.debug(LTSV.dump("Memcached" -> "transformAndGet",
        "key" -> key,
        "is" -> truncateValue(value),
        "duration" -> exp.toString))
    }
    f
  }
}
