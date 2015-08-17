package com.m3.octoparts.json.format

import java.nio.charset.Charset

import org.scalatest._
import scala.collection.convert.Wrappers.JCollectionWrapper
import scala.concurrent.duration._

class CustomFormattersSpec extends FunSpec with Matchers with CustomFormatters {

  describe("durationFormat") {
    it("should serialise and deserialise a duration") {
      val duration = 3.seconds
      val json = durationFormat.writes(duration)
      val des = durationFormat.reads(json).get
      des should be(duration)
    }
  }

  describe("charsetFormat") {
    it("should serialise and deserialise a charset") {
      JCollectionWrapper(Charset.availableCharsets().values()).foreach { charset =>
        val json = charsetFormat.writes(charset)
        val des = charsetFormat.reads(json).get
        des should be(charset)
      }
    }
  }
}
