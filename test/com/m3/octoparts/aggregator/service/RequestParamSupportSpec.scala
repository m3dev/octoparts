package com.m3.octoparts.aggregator.service

import com.m3.octoparts.aggregator.PartRequestInfo
import com.m3.octoparts.config._
import com.m3.octoparts.model._
import com.m3.octoparts.model.config.ParamType._
import com.m3.octoparts.model.config._
import com.m3.octoparts.support.db.RequiresDB
import org.scalatest.{ FunSpec, Matchers }

class RequestParamSupportSpec extends FunSpec with Matchers with RequiresDB with RequestParamSupport {

  describe("#combineParams") {
    it("should combine the registered parameters across the PartRequest params and RequestMeta") {
      val pathParam1 = PartParam(required = true, versioned = false, paramType = Query, outputName = "meta.requestUrl", updatedAt = now, createdAt = now)
      val pathParam2 = PartParam(required = true, versioned = false, paramType = Query, outputName = "requestUrl", updatedAt = now, createdAt = now)
      val reqMeta = RequestMeta("leId", userAgent = Some("leAgent"), requestUrl = Some("http://localhost"))
      val partReq = PartRequest("partId", params = Seq(PartRequestParam("requestUrl", "uzer")))
      val registeredParams = Set(pathParam1, pathParam2)
      val expected = Map(
        ShortPartParam(pathParam1) -> Seq("http://localhost"),
        ShortPartParam(pathParam2) -> Seq("uzer")
      )
      combineParams(registeredParams, PartRequestInfo(reqMeta, partReq)) should equal(expected)
    }
  }

  describe("#processMeta") {
    it("should return a list with the predefined keys") {
      val reqMeta = RequestMeta("leId", userAgent = Some("leAgent"), serviceId = Some("myService"), requestUrl = Some("http://localhost"))
      val expected = Seq(
        PartRequestParam("meta.userAgent", "leAgent"),
        PartRequestParam("meta.serviceId", "myService"),
        PartRequestParam("meta.requestUrl", "http://localhost")
      )
      processMeta(reqMeta).diff(expected) should have size 0
    }
    it("should return a list without userAgent if not provided") {
      processMeta(RequestMeta("leId")) should equal(Nil)
    }
  }
}
