package com.m3.octoparts.future

import java.util.concurrent.{ Executors, TimeUnit }

import akka.actor.ActorSystem
import com.google.common.util.concurrent.ThreadFactoryBuilder

import scala.concurrent._
import scala.concurrent.duration.{ Duration, FiniteDuration }
import scala.language.implicitConversions

/**
 * Companion object with an implicit conversion to RichFutureWithTimeout in addition
 * to a field member holding the ExecutionContext used by that class
 */
object RichFutureWithTimeout {

  val actorSystem = ActorSystem("FutureTimeoutSystem")

  /*
    Using a CachedThreadPool because this is for spawning many short-lived futures that just block
    until the timeout time is over
  */
  val timeoutEC = {
    val namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("future-timeout-%d").build()
    ExecutionContext.fromExecutor(Executors.newCachedThreadPool(namedThreadFactory))
  }

  /**
   * Implicit conversion between Future[A] and RichFutureWithTimeout[A]
   * @param f The Future to convert
   * @tparam A Future parameterised value
   * @return RichFutureWithTimeout[A]
   */
  implicit def toRichFutureWithTimeout[A](f: Future[A]): RichFutureWithTimeout[A] = new RichFutureWithTimeout(f)
}

/**
 * Rich Future with timeout support on an individual Future basis
 *
 * Extends from AnyVal for zero run-time conversion penalties.
 * Should really be instantiated via implicit conversion from a Future instead.
 */
class RichFutureWithTimeout[+A](val f: Future[A]) extends AnyVal {

  import com.m3.octoparts.future.RichFutureWithTimeout.{ actorSystem, timeoutEC }

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