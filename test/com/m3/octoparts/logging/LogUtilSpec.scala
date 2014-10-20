package com.m3.octoparts.logging

import org.scalatest._

import scala.concurrent.duration._

class LogUtilSpec extends FunSpec with Matchers with LogUtil {

  describe("#toRelevantUnit") {
    it("should use nanos") {
      toRelevantUnit(0.nanos) should be(0.nanos)
      toRelevantUnit(123.nanos) should be(123.nanos)
    }
    it("should use micros") {
      toRelevantUnit(1234.nanos) should be(1.micro)
      toRelevantUnit(123.micros) should be(123.micros)
    }
    it("should use millis") {
      toRelevantUnit(1234.micros) should be(1.milli)
      toRelevantUnit(123.millis) should be(123.millis)
    }
    it("should use seconds") {
      toRelevantUnit(1234.millis) should be(1.second)
      toRelevantUnit(12.seconds) should be(12.seconds)
    }
    it("should use minutes") {
      toRelevantUnit(123.seconds) should be(2.minutes)
      toRelevantUnit(12.minutes) should be(12.minutes)
    }
    it("should use hours") {
      toRelevantUnit(123.minutes) should be(2.hours)
      toRelevantUnit(123.hours) should be(123.hours)
    }
  }
}
