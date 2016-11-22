package com.m3.octoparts.future

import com.kenshoo.play.metrics.Metrics

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.implicitConversions

object RichFutureWithTiming {

  /**
   * Enrichment for scala.concurrent.Future adding methods for measuring execution time.
   */
  implicit class RichFutureWithTimingOps[A](
      val future: Future[A]
  ) extends AnyVal {

    /**
     * Measures time (the `tapper` callback is called asynchronously)
     *
     * @param tapper a callback that uses the future's result and time taken, e.g. for performance logging
     */
    def time(tapper: (A, FiniteDuration) => Unit)(implicit executionContext: ExecutionContext): Future[A] = {
      val startNanos = System.nanoTime()
      future.onSuccess {
        case r => tapper(r, Duration.fromNanos(System.nanoTime() - startNanos))
      }
      future
    }

    /**
     * Measures time too (the `f` callback is called synchronously and its result it passed down the future chain)
     *
     * @param f a callback that takes the future's result and time taken, and transforms them into some other result
     */
    def timeAndTransform[B](f: (A, FiniteDuration) => B)(implicit executionContext: ExecutionContext): Future[B] = {
      val startNanos = System.nanoTime()
      future.map {
        r =>
          f(r, Duration.fromNanos(System.nanoTime() - startNanos))
      }
    }

    def measure(metricsName: String)(implicit executionContext: ExecutionContext, metrics: Metrics): Future[A] = time {
      (_, duration) => metrics.defaultRegistry.timer(metricsName).update(duration.length, duration.unit)
    }
  }
}
