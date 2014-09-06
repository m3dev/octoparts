package com.m3.octoparts

import com.m3.octoparts.JsonFormats._
import org.scalatest.{ FunSpec, Matchers }

import scala.concurrent.duration._

/**
 * Created by Lloyd on 9/6/14.
 */
class JsonFormatsSpec extends FunSpec with Matchers {

  describe("durationFormat") {

    it("should serialise and deserialise a duration") {
      val duration = 3.seconds
      val json = durationFormat.writes(duration)
      val des = durationFormat.reads(json).get
      des should be(duration)
    }

  }

  describe("enumFormat") {

    it("should create a Format that can be used to serialise and deserialise an enum") {
      object SimpleEnum extends Enumeration {
        val A, B, C = Value
      }
      val simpleEnumFormat = enumFormat(SimpleEnum)
      val b = SimpleEnum.B
      val json = simpleEnumFormat.writes(b)
      val des = simpleEnumFormat.reads(json).get
      des should be(b)
    }

  }

}
