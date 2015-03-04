package com.m3.octoparts.client

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

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
        HttpMethod.Post, HystrixConfig(5.seconds, ThreadPoolConfig("knitty", 1, 10), "p", "knitty", true),
        httpPoolSize = 5, httpConnectionTimeout = 1.second, httpSocketTimeout = 5.seconds, httpDefaultEncoding = StandardCharsets.UTF_8.name(),
        alertMailSettings = AlertMailSettings.Off))
      val serialized = OctopartsApiBuilder.Mapper.writeValueAsBytes(configs)
      EndpointListExtractor.deserialize(new ByteArrayInputStream(serialized)) should equal(SeqWrapper(configs))
    }
  }
}
