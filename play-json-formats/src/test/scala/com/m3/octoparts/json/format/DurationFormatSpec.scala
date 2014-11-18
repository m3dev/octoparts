package com.m3.octoparts.json.format

import org.scalatest._
import scala.concurrent.duration._

class DurationFormatSpec extends FunSpec with Matchers {

  describe("durationFormat") {
    import DurationFormat.durationFormat

    it("should serialise and deserialise a duration") {
      val duration = 3.seconds
      val json = durationFormat.writes(duration)
      val des = durationFormat.reads(json).get
      des should be(duration)
    }

  }
}
