package com.m3.octoparts.client

import com.m3.octoparts.model.AggregateRequest
import org.scalatest.{ BeforeAndAfterAll, FunSpec, Matchers }

class OctopartsApiBuilderTest extends FunSpec with BeforeAndAfterAll with Matchers {

  val apiBuilder = new OctopartsApiBuilder("http://octoparts/", "m3.com")
  it("should prepare a POST") {

    val request = apiBuilder.newRequest("123", "cafebabe", null, "/index.jsp", 456L)
    request.newPart("part1", null).addParam("q", "lookForThis").build()
    val agr = request.build
    val ningrequest = apiBuilder.toHttp(agr)
    ningrequest.getBodyEncoding should be("UTF-8")
    OctopartsApiBuilder.Mapper.readValue(ningrequest.getByteData, classOf[AggregateRequest]) should be(agr)
    ningrequest.getMethod should be("POST")
    ningrequest.getRawUrl should startWith("http://octoparts/")
  }

  it("should escape var arguments, handling nulls") {
    OctopartsApiBuilder.formatWithUriEscape("%s", " ") should be("%20")
    OctopartsApiBuilder.formatWithUriEscape("%s%s", " ", " ") should be("%20%20")
    OctopartsApiBuilder.formatWithUriEscape("%s%s", null, " ") should be("null%20")
  }

  override def afterAll() {
    apiBuilder.close()
  }
}
