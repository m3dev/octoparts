package com.m3.octoparts.util

import org.scalatest.{ FlatSpec, Matchers }

class TimingSupportSpec extends FlatSpec with Matchers with TimingSupport {
  behavior of "TimingSupport"

  it should "give reasonably correct timings" in {

    time {
      Thread.sleep(100L)
      "Hello"
    } { (result, duration) =>
      result should be("Hello")
      duration.toMillis should be >= 90L
      duration.toMillis should be <= 120L
    }

  }
}
