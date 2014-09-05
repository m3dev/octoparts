package com.m3.octoparts.cache

import org.scalatest._

class CacheCodecsSpec extends FunSpec with Matchers {

  describe(".optionCodecFor") {
    import CacheCodecs.optionCodecFor

    it("""should generate a codec that doesn't break with Some("None")""") {
      val none: Option[String] = None
      val some: Option[String] = Some("None")
      val optionStringCodec = optionCodecFor[String]
      val noneBytes = optionStringCodec.serialize(none)
      val someBytes = optionStringCodec.serialize(some)
      val deserialisedNone = optionStringCodec.deserialize(noneBytes)
      val deserialisedSome = optionStringCodec.deserialize(someBytes)
      deserialisedNone should be(None)
      deserialisedSome should be(Some("None"))
    }

  }

}
