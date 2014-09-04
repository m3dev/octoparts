package com.m3.octoparts.future

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ FlatSpec, Matchers }

import scala.concurrent.Future
import scala.concurrent.duration.Duration

class RichFutureWithTimingSpec extends FlatSpec with Matchers with ScalaFutures {
  import scala.concurrent.ExecutionContext.Implicits.global

  behavior of "RichFutureWithTiming"

  it should "not start measuring too early" in {
    import com.m3.octoparts.future.RichFutureWithTiming._

    val delayBeforeStartTiming = 50L

    // The future starts running now
    def createMyFuture = Future {
      val startNanos = System.nanoTime()
      Thread.sleep(100L)
      startNanos
    }

    // Wait a while before starting the timing
    Thread.sleep(delayBeforeStartTiming)

    val future = createMyFuture.timeAndTransform {
      case (result, duration) =>
        val nowNanos = System.nanoTime()
        val tt = Duration.fromNanos(nowNanos - result).toMillis

        // the actual time (=delay between starting the future and calling the f) should be slightly over 100
        tt should be >= 100L
        tt should be < 120L

        // the recorded time (=known to the f) should be slightly over (the actual time - the delay before starting the timer)
        duration.toMillis should be >= (tt - delayBeforeStartTiming)
        duration.toMillis - tt - delayBeforeStartTiming should be < 10L
        "OK"
    }

    whenReady(future) { result =>
      result should be("OK")
    }
  }
}
