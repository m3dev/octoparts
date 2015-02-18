package com.m3.octoparts.client

import com.m3.octoparts.model.{ AggregateRequest, PartRequestParam }
import org.scalatest.{ BeforeAndAfterAll, FunSpec, Matchers }

import scala.concurrent.duration._

class RequestBuilderTest extends FunSpec with BeforeAndAfterAll with Matchers {

  val apiBuilder = new OctopartsApiBuilder("http://octoparts/", "m3.com")
  it("should build a request") {

    val request = apiBuilder.newRequest("123", "cafebabe", null, "/index.jsp", 456L)
    request.newPart("part1", null).addParam("q", "lookForThis").build()

    val ag = request.build
    ag.requestMeta.serviceId should be(Some("m3.com"))
    ag.requestMeta.userAgent should be(None)
    ag.requestMeta.userId should be(Some("123"))
    ag.requestMeta.sessionId should be(Some("cafebabe"))
    ag.requestMeta.timeout should be(Some(456.millis))
    ag.requestMeta.requestUrl should be(Some("/index.jsp"))
    ag.getRequests should have size 1

    val part1Params = ag.requests.find(_.partId == "part1").toSeq.flatMap(_.params)
    part1Params should have size 1
    part1Params.head should be(PartRequestParam("q", "lookForThis"))
  }

  it("should serialize and deserialize") {
    val request = apiBuilder.newRequest("123", "cafebabe", null, "/index.jsp", 456L)
    request.newPart("part1", null).addParam("q", "lookForThis").build()
    val ag = request.build

    val serialized = OctopartsApiBuilder.Mapper.writeValueAsBytes(ag)
    OctopartsApiBuilder.Mapper.readValue(serialized, classOf[AggregateRequest]) shouldBe ag
  }

  override def afterAll() {
    apiBuilder.close()
  }
}
