package com.m3.octoparts.future

import java.util.concurrent.{ Executors, TimeUnit }

import akka.actor.ActorSystem
import com.google.common.util.concurrent.ThreadFactoryBuilder

import scala.concurrent.{ Future, ExecutionContext, Promise, TimeoutException }
import scala.concurrent.duration.{ Duration, FiniteDuration }

object RichFutureWithTimeout {

  private val actorSystem = ActorSystem("FutureTimeoutSystem")

  /*
    Using a CachedThreadPool because this is for spawning many short-lived futures that just block
    until the timeout time is over
  */
  private val timeoutEC = {
    val namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("future-timeout-%d").build()
    ExecutionContext.fromExecutor(Executors.newCachedThreadPool(namedThreadFactory))
  }

  /**
   * Rich Future with timeout support on an individual Future basis
   *
   * Extends from AnyVal for zero run-time conversion penalties.
   * Should really be instantiated via implicit conversion from a Future instead.
   */
  implicit class RichFutureWithTimeoutOps[A](val f: Future[A]) extends AnyVal {

    /**
     * An enrichment method for Future[A] that allows us to specify an
     * arbitrary timeout on the Future on which this method is called without
     * affecting (e.g. cancelling) the original Future in any way.
     *
     * @param timeout Duration until the Future is timed out
     * @return a Future[A]
     */
    def timeoutIn(timeout: Duration): Future[A] = {
      val p = Promise[A]()
      val cancellable = actorSystem.scheduler.scheduleOnce(FiniteDuration(timeout.toMillis, TimeUnit.MILLISECONDS)) {
        p.tryFailure(new TimeoutException(s"Timed out after $timeout"))
      }(timeoutEC)
      f.onComplete { r =>
        cancellable.cancel()
        p.tryComplete(r)
      }(timeoutEC)
      p.future
    }

  }
}