package com.m3.octoparts.client

import java.nio.charset.StandardCharsets

import com.fasterxml.jackson.databind.ObjectReader
import com.m3.octoparts.model.config.ParamType
import com.m3.octoparts.model.{ HttpMethod, AggregateRequest }
import com.m3.octoparts.model.config.json.HttpPartConfig
import org.scalatest.{ BeforeAndAfterAll, FunSpec, Matchers }

import scala.collection.convert.Wrappers.JListWrapper

class OctopartsApiBuilderTest extends FunSpec with BeforeAndAfterAll with Matchers {

  val apiBuilder = new OctopartsApiBuilder("http://octoparts/", "m3.com")

  it("should prepare a POST") {
    val request = apiBuilder.newRequest("123", "cafebabe", null, "/index.jsp", 456L)
    request.newPart("part1", null).addParam("q", "lookForThis").build()
    val agr = request.build
    val ningrequest = apiBuilder.toHttp(agr)
    ningrequest.getBodyEncoding should be(StandardCharsets.UTF_8.name())
    OctopartsApiBuilder.Mapper.readValue(ningrequest.getByteData, classOf[AggregateRequest]) should be(agr)
    ningrequest.getMethod should be("POST")
    ningrequest.getUrl should startWith("http://octoparts/")
  }

  it("should pass headers") {
    val request = apiBuilder.newRequest("123", "cafebabe", null, "/index.jsp", 456L)
    val agr = request.build
    val ningrequest = apiBuilder.toHttp(agr, "a" -> "b")
    JListWrapper(ningrequest.getHeaders.get("a")).toSeq shouldBe Seq("b")
  }

  it("should escape var arguments, handling nulls") {
    OctopartsApiBuilder.formatWithUriEscape("%s", " ") should be("%20")
    OctopartsApiBuilder.formatWithUriEscape("%s%s", " ", " ") should be("%20%20")
    OctopartsApiBuilder.formatWithUriEscape("%s%s", null, " ") should be("null%20")
  }

  it("should decode enums with Jackson") {
    val source = """{
               |    "partId": "test",
               |    "owner": "tiger",
               |    "uriToInterpolate": "http://local.host/",
               |    "description": "",
               |    "method": "put",
               |    "hystrixConfig": {
               |      "timeout": 5000,
               |      "threadPoolConfig": {
               |        "threadPoolKey": "swimming",
               |        "coreSize": 5,
               |        "queueSize": 256
               |      },
               |      "commandKey": "test",
               |      "commandGroupKey": "swimming"
               |    },
               |    "additionalValidStatuses": [],
               |    "parameters": [
               |      {
               |        "required": false,
               |        "versioned": false,
               |        "paramType": "header",
               |        "outputName": "limit",
               |        "cacheGroups": []
               |      }
               |    ],
               |    "cacheGroups": [],
               |    "cacheTtl": 0,
               |    "alertMailsEnabled": false,
               |    "alertPercentThreshold": 5,
               |    "alertInterval": 60000
               |  }""".stripMargin
    val reader: ObjectReader = OctopartsApiBuilder.Mapper.reader(classOf[HttpPartConfig])
    val partConfig = reader.readValue[HttpPartConfig](source)
    partConfig.method should be(HttpMethod.Put)
    partConfig.parameters.head.paramType should be(ParamType.Header)
  }

  override def afterAll() {
    apiBuilder.close()
  }
}
