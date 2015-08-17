package com.m3.octoparts.json

import org.scalatest._
import play.api.libs.json.JsString

class EnumerationHelperSpec extends FunSpec with Matchers {

  object SimpleEnum extends Enumeration {
    val A, B, C = Value
  }

  describe(".writes") {

    it("should create a Writes that can be used to serialise an enum") {
      val simpleWrites = EnumerationHelper.writes[SimpleEnum.type]
      val a = SimpleEnum.A
      val ser = simpleWrites.writes(a)
      ser should be(JsString("A"))
    }

  }

  describe(".reads") {

    it("should create a Reads that can be used to deserialise an enum") {
      val simpleReads = EnumerationHelper.reads(SimpleEnum)
      val des = simpleReads.reads(JsString("B")).get
      val b = SimpleEnum.B
      des should be(b)
    }

  }

  describe(".formats") {

    it("should create a Format that can be used to serialise and deserialise an enum") {
      val simpleEnumFormat = EnumerationHelper.formats(SimpleEnum)
      val c = SimpleEnum.C
      val json = simpleEnumFormat.writes(c)
      val des = simpleEnumFormat.reads(json).get
      des should be(c)
    }

  }
}
