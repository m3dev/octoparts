package com.m3.octoparts.future

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem

import scala.concurrent.{ Promise => SPromise }
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ Future, ExecutionContext }
import scala.util.Try

trait PromiseSupport {

  /**
   * The injected actor system
   * @return
   */
  def actorSystem: ActorSystem

  /**
   * Constructs a Future which will contain value "message" after the given duration elapses.
   * This is useful only when used in conjunction with other Promises
   * @param message message to be displayed
   * @param duration duration for the scheduled promise
   * @return a scheduled promise
   */
  def timeout[A](message: => A, duration: scala.concurrent.duration.Duration)(implicit ec: ExecutionContext): Future[A] = {
    timeout(message, duration.toMillis)
  }

  /**
   * Constructs a Future which will contain value "message" after the given duration elapses.
   * This is useful only when used in conjunction with other Promises
   * @param message message to be displayed
   * @param duration duration for the scheduled promise
   * @return a scheduled promise
   */
  def timeout[A](message: => A, duration: Long, unit: TimeUnit = TimeUnit.MILLISECONDS)(implicit ec: ExecutionContext): Future[A] = {
    val p = SPromise[A]()
    actorSystem.scheduler.scheduleOnce(FiniteDuration(duration, unit)) {
      p.complete(Try(message))
    }
    p.future
  }

}
