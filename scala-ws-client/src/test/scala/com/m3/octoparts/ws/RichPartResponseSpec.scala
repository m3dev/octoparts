package com.m3.octoparts.ws

import com.m3.octoparts.model.PartResponse
import org.scalatest.{FunSpec, Matchers}

import scala.util.Success

class RichPartResponseSpec extends FunSpec with Matchers {
  import AggregateResponseEnrichment.RichPartResponse
  describe("#tryContents") {
    it("should fail on blank contents") {
      val partResponse = PartResponse("partId", "id", contents = Some(" \r\n "))
      partResponse.tryContents.isFailure shouldBe true
    }
  }

  describe("#tryContentsIfNoError") {
    it ("should stop if there are errors") {
      val partResponse = PartResponse("partId", "id", contents = Some("woot!"), errors = Seq("Oops"))
      partResponse.tryContentsIfNoError.isFailure shouldBe true
    }
    it ("should not stop for mere warnings") {
      val partResponse = PartResponse("partId", "id", contents = Some("woot!"), warnings = Seq("Oops"))
      partResponse.tryContentsIfNoError shouldBe Success("woot!")
    }
  }
}
