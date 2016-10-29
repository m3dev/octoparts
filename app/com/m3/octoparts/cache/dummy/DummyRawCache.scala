package com.m3.octoparts.cache.dummy

import com.m3.octoparts.cache.RawCache
import com.twitter.zipkin.gen.Span
import shade.memcached.Codec

import scala.concurrent.Future
import scala.concurrent.duration.Duration

object DummyRawCache extends RawCache {

  def get[T](
    key: String
  )(implicit
    codec: Codec[T],
    parentSpan: Span): Future[Option[T]] = Future.successful(None)

  def set[T](
    key: String,
    value: T,
    exp: Duration
  )(implicit
    codec: Codec[T],
    parentSpan: Span): Future[Unit] = Future.successful(())

  def close(): Unit = {}

}
