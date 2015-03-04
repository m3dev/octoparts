package com.m3.octoparts.model.config

import java.nio.charset.StandardCharsets

import com.m3.octoparts.model.HttpMethod
import com.m3.octoparts.model.config.json.AlertMailSettings
import org.apache.commons.lang3.SerializationUtils
import scala.concurrent.duration._
import com.m3.octoparts.support.mocks.ConfigDataMocks
import scala.language.postfixOps
import org.scalatest._

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
        description = None,
        method = HttpMethod.Get,
        hystrixConfig = json.HystrixConfig(
          timeout = 50 millis,
          threadPoolConfig = json.ThreadPoolConfig(
            threadPoolKey = "testThreadPool",
            coreSize = 2,
            queueSize = 256),
          commandKey = "command",
          commandGroupKey = "GroupKey",
          false),
        additionalValidStatuses = Set(302),
        httpPoolSize = 5,
        httpConnectionTimeout = 1.second,
        httpSocketTimeout = 5.seconds,
        httpDefaultEncoding = StandardCharsets.US_ASCII,
        httpProxy = Some("localhost:666"),
        parameters = Set(
          json.PartParam(
            required = true,
            versioned = false,
            paramType = ParamType.Header,
            outputName = "userId",
            inputNameOverride = None,
            cacheGroups = Set.empty
          )),
        deprecatedInFavourOf = None,
        cacheGroups = Set(mockCacheGroup).map(CacheGroup.toJsonModel),
        cacheTtl = Some(60 seconds),
        alertMailSettings = AlertMailSettings(
          alertMailsEnabled = true,
          alertAbsoluteThreshold = Some(1000),
          alertPercentThreshold = Some(33.0),
          alertInterval = 10 minutes,
          alertMailRecipients = Some("l-chan@m3.com")
        ),
        localContentsEnabled = true,
        localContents = Some("{}"))
      jsonModel should be(expectedModel)
    }

  }

  describe("Java serdes (for caching)") {

    it("should serialise and deserialise without problems") {
      val original = mockHttpPartConfig.copy(
        hystrixConfig = Some(mockHystrixConfig),
        additionalValidStatuses = Set(302),
        cacheGroups = Set(mockCacheGroup)
      )
      val serialised = SerializationUtils.serialize(original)
      val deserialised = SerializationUtils.deserialize[HttpPartConfig](serialised)
      deserialised shouldBe original
    }

  }

}
