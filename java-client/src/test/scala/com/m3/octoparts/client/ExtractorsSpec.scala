package com.m3.octoparts.client

import java.io.ByteArrayInputStream

import com.m3.octoparts.model.config.json._
import com.m3.octoparts.model.{ HttpMethod, ResponseMeta, AggregateResponse }
import org.scalatest.{ Matchers, FunSpec }

import scala.collection.convert.Wrappers.SeqWrapper
import scala.concurrent.duration._

class ExtractorsSpec extends FunSpec with Matchers {
  describe("AggregateResponseExtractor") {
    it("should extract something that has just been serialized") {
      val ar = AggregateResponse(ResponseMeta("123", 10.seconds))
      val serialized = OctopartsApiBuilder.Mapper.writeValueAsBytes(ar)
      val wrapper = AggregateResponseExtractor.deserialize(new ByteArrayInputStream(serialized))
      wrapper.getResponseMeta should be(ar.responseMeta)
      wrapper.getPartResponses.iterator().hasNext shouldBe false
    }
  }
  describe("EndpointListExtractor") {
    it("should extract something that has just been serialized") {
      val configs = Seq(HttpPartConfig("p", "me", "http://localhost", Some(""),
        HttpMethod.Post, HystrixConfig(5.seconds, ThreadPoolConfig("knitty", 1, 10), "p", "knitty", false), alertMailsEnabled = false, alertInterval = 50.minutes))
      val serialized = OctopartsApiBuilder.Mapper.writeValueAsBytes(configs)
      EndpointListExtractor.deserialize(new ByteArrayInputStream(serialized)) should equal(SeqWrapper(configs))
    }
  }
}
