package com.m3.octoparts.model.config

import com.m3.octoparts.model.HttpMethod
import scala.concurrent.duration._
import com.m3.octoparts.support.mocks.ConfigDataMocks
import org.scalatest._

/**
 * Created by Lloyd on 9/6/14.
 */
class HttpPartConfigSpec extends FunSpec with Matchers with ConfigDataMocks {

  describe(".toJsonModel") {

    it("should throw an IllegalArgumentException when given a config without a HystrixConfig") {
      intercept[IllegalArgumentException] {
        HttpPartConfig.toJsonModel(mockHttpPartConfig)
      }
    }

    it("should not throw when given a config with a HystrixConfig") {
      val jsonModel = HttpPartConfig.toJsonModel(mockHttpPartConfig.copy(
        hystrixConfig = Some(mockHystrixConfig),
        additionalValidStatuses = Set(302),
        cacheGroups = Set(mockCacheGroup)
      ))
      val expectedModel = json.HttpPartConfig(
        partId = "something",
        owner = "somebody",
        uriToInterpolate = "http://random.com",
        description = "",
        method = HttpMethod.Get,
        hystrixConfig = json.HystrixConfig(
          timeoutInMs = 50,
          threadPoolConfig = json.ThreadPoolConfig(
            threadPoolKey = "testThreadPool",
            coreSize = 2,
            queueSize = 256),
          commandKey = "command",
          commandGroupKey = "GroupKey"),
        additionalValidStatuses = Set(302),
        parameters = Set(
          json.PartParam(
            required = true,
            versioned = false,
            paramType = ParamType.Header,
            outputName = "userId",
            inputNameOverride = None,
            cacheGroups = Set()
          )),
        deprecatedInFavourOf = None,
        cacheGroups = Set(mockCacheGroup).map(CacheGroup.toJsonModel),
        cacheTtl = Some(60 seconds),
        alertMailsEnabled = true,
        alertAbsoluteThreshold = Some(1000),
        alertPercentThreshold = Some(33.0),
        alertInterval = 10 minutes,
        alertMailRecipients = Some("l-chan@m3.com"))
      jsonModel should be(expectedModel)
    }

  }

}
