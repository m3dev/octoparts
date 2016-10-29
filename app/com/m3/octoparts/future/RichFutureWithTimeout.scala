package com.m3.octoparts.future

import akka.actor.ActorSystem
import play.api.{ Mode, Play }

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ Future, Promise, TimeoutException }
import scala.util.control.NonFatal

object RichFutureWithTimeout {

  private val actorSystem = ActorSystem("future-timeout-actor-system")

  /*
   * this execution context is dedicated to scheduling future timeouts
  **/
  private val timeoutEC = try {
    actorSystem.dispatchers.lookup("contexts.future-timeout")
  } catch {
    // for tests
    case NonFatal(e) if Play.maybeApplication.fold(false)(_.mode == Mode.Test) => actorSystem.dispatcher
  }

  implicit class RichFutureWithTimeoutOps[A](
      val f: Future[A]
  ) extends AnyVal {

    /**
     * An enrichment method for Future[A] that allows us to specify an
     * arbitrary timeout on the Future on which this method is called without
     * affecting (e.g. cancelling) the original Future in any way.
     *
     * @param timeout Duration until the Future is timed out
     * @return a Future[A]
     */
    def timeoutIn(timeout: FiniteDuration): Future[A] = {
      val p = Promise[A]()

      // default values for the scheduler (10ms tick, 512 wheel size) is fine
      val cancellable = actorSystem.scheduler.scheduleOnce(timeout) {
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
