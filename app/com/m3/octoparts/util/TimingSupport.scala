package com.m3.octoparts.util

import play.api.Logger

import scala.concurrent.duration._

/**
 * Various timing utilities for synchronous processing.
 *
 * For help with adding timing to Futures, take a look at the `com.m3.octoparts.future` package.
 */
trait TimingSupport {

  /**
   * measures the execution time of f, then passes it to withTime, and finally returns f's return value
   */
  def time[A](f: => A)(withTime: (A, Duration) => Unit): A = {
    def withTime2(a: A, duration: Duration): A = {
      withTime(a, duration)
      a
    }
    timeAndTransform(f)(withTime2)
  }

  /**
   * measures the execution time of f, then transform its result with `withTime`
   */
  def timeAndTransform[A, B](f: => A)(withTime: (A, Duration) => B): B = {
    val startTimeMs = System.currentTimeMillis()
    val result = f
    val endTimeMs = System.currentTimeMillis()
    withTime(result, (endTimeMs - startTimeMs).millis)
  }

  def timeTrace[A](f: => A)(opName: String): A = {
    Logger.trace(s"Start $opName")
    time(f) {
      (a, duration) => Logger.trace(s"End $opName: $duration")
    }
  }
}
