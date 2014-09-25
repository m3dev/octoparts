package com.m3.octoparts.aggregator.service

import com.m3.octoparts.aggregator.PartRequestInfo
import com.m3.octoparts.model._
import com.m3.octoparts.model.config.ParamType._
import com.m3.octoparts.model.config._
import com.m3.octoparts.support.db.RequiresDB
import org.joda.time.DateTime.now
import org.scalatest.{ FunSpec, Matchers }

class RequestParamSupportSpec extends FunSpec with Matchers with RequiresDB with RequestParamSupport {

  describe("#combineParams") {
    it("should combine the registered parameters across the PartRequest params and RequestMeta") {
      val pathParam1 = PartParam(required = true, versioned = false, paramType = Query, outputName = "meta.requestUrl", updatedAt = now, createdAt = now)
      val pathParam2 = PartParam(required = true, versioned = false, paramType = Query, outputName = "requestUrl", updatedAt = now, createdAt = now)
      val reqMeta = RequestMeta("leId", userAgent = Some("leAgent"), requestUrl = Some("http://localhost"))
      val partReq = PartRequest("partId", params = Set(PartRequestParam("requestUrl", "uzer")))
      val registeredParams = Set(pathParam1, pathParam2)
      val expected = Map(
        pathParam1.shorter -> "http://localhost",
        pathParam2.shorter -> "uzer"
      )
      combineParams(registeredParams, PartRequestInfo(reqMeta, partReq)) should equal(expected)
    }

    it("should discard null values") {
      val reqMeta = RequestMeta("leId", userAgent = null, requestUrl = Some("http://localhost"), serviceId = Some(null))
      val partReq = PartRequest("partId", params = Set(PartRequestParam("requestUrl", null)))
      val registeredParams = Set(
        PartParam(required = false, versioned = false, paramType = Query, outputName = "requestUrl", updatedAt = now, createdAt = now),
        PartParam(required = false, versioned = false, paramType = Query, outputName = "meta.requestUrl", updatedAt = now, createdAt = now))
      val expected = Map(
        ShortPartParam("meta.requestUrl", Query) -> "http://localhost"
      )
      combineParams(registeredParams, PartRequestInfo(reqMeta, partReq)) should equal(expected)
    }
  }

  describe("#processMeta") {
    it("should return a map with the predefined keys") {
      val reqMeta = RequestMeta("leId", userAgent = Some("leAgent"), serviceId = Some("myService"), requestUrl = Some("http://localhost"))
      val expected = Map(
        "meta.userAgent" -> "leAgent",
        "meta.serviceId" -> "myService",
        "meta.requestUrl" -> "http://localhost"
      )
      processMeta(reqMeta) should equal(expected)
    }
    it("should return a map without userAgent if not provided") {
      val expected = Map.empty
      processMeta(RequestMeta("leId")) should be(expected)
    }
  }

  describe("repeated parameters related") {
    val repeatedKey = "thing"
    val repeatedParams = Set(PartRequestParam(repeatedKey, "v1"), PartRequestParam(repeatedKey, "v2"))
    val nonRepeatedParams = Set(PartRequestParam("thing2", "v"), PartRequestParam("thing3", "v4"))
    val allParams = repeatedParams ++ nonRepeatedParams

    describe("#repeatedParamKeys") {
      it("should identify repeated keys only") {
        val rpk = repeatedParamKeys(allParams).toSeq
        rpk.contains(repeatedKey) should be(true)
        rpk.size should be(1)
      }
    }

    describe("#ensureUniqueParamKeys") {
      it("should throw an IllegalArgumentException if passed a PartRequest with repeated para ms") {
        intercept[IllegalArgumentException] {
          ensureUniqueParamKeys(allParams)
        }
      }
      it("should not throw an IllegalArgumentException if passed a PartRequest without repeated params") {
        ensureUniqueParamKeys(nonRepeatedParams)
      }
    }
  }

}
