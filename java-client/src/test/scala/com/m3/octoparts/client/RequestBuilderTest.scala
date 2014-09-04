package com.m3.octoparts.client

import com.m3.octoparts.model.PartRequestParam
import org.scalatest.{ BeforeAndAfterAll, FunSpec, Matchers }

class RequestBuilderTest extends FunSpec with BeforeAndAfterAll with Matchers {

  val apiBuilder = new OctopartsApiBuilder("http://octoparts/", "m3.com")
  it("should build a request") {

    val request = apiBuilder.newRequest("123", "cafebabe", null, "/index.jsp", 456L)
    request.newPart("part1", null).addParam("q", "lookForThis").build()
    request.countParts should be(1)

    val ag = request.build
    ag.requestMeta.serviceId should be(Some("m3.com"))
    ag.requestMeta.userAgent should be(None)
    ag.requestMeta.userId should be(Some("123"))
    ag.requestMeta.sessionId should be(Some("cafebabe"))
    ag.requestMeta.timeoutMs should be(Some(456L))
    ag.requestMeta.requestUrl should be(Some("/index.jsp"))
    ag.getRequests should have size 1

    val part1Params = ag.requests.find(_.partId == "part1").map(_.params.toSeq).getOrElse(Nil)
    part1Params should have size 1
    part1Params.head should be(PartRequestParam("q", "lookForThis"))
  }

  override def afterAll() {
    apiBuilder.close()
  }
}
