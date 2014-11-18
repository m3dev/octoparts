package com.m3.octoparts.json.format

import java.nio.charset.Charset

import org.scalatest._

import scala.collection.convert.Wrappers.JCollectionWrapper

class CharsetFormatSpec extends FunSpec with Matchers {

  describe("charsetFormat") {
    import com.m3.octoparts.json.format.CharsetFormat.charsetFormat
    it("should serialise and deserialise a charset") {
      JCollectionWrapper(Charset.availableCharsets().values()).foreach { charset =>
        val json = charsetFormat.writes(charset)
        val des = charsetFormat.reads(json).get
        des should be(charset)
      }
    }

  }
}
