package com.m3.octoparts.client

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.duration._

class DurationModuleSpec extends FunSpec with Matchers {

  private object DurationModule extends DurationModule

  private val mapper = new ObjectMapper()
  mapper.registerModule(DurationModule)

  describe("DurationModule") {
    it("should serialize a duration") {
      mapper.writeValueAsString(10.millis) should be("10")
      mapper.writeValueAsString(10.minutes) should be("600000")
    }
    it("should deserialize a duration") {
      mapper.readValue[Duration]("90", classOf[Duration]) should be(90.millis)
      mapper.readValue[Duration]("5400000", classOf[Duration]) should be(90.minutes)
    }
    it("should fail to deserialize an invalid duration") {
      a[JsonProcessingException] should be thrownBy {
        mapper.readValue[Duration]("90.0", classOf[Duration])
      }
      a[JsonProcessingException] should be thrownBy {
        mapper.readValue[Duration]("'abc'", classOf[Duration])
      }
      a[JsonProcessingException] should be thrownBy {
        mapper.readValue[Duration]("{10}", classOf[Duration])
      }
    }
  }
}
