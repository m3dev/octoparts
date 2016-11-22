package controllers

import com.m3.octoparts.model.config.{ PartParam, CacheGroup }
import com.m3.octoparts.support.mocks.ConfigDataMocks
import controllers.AdminForms._
import org.scalatest.{ FunSpec, Matchers }

import scala.collection.SortedSet

class AdminFormsSpec extends FunSpec with Matchers with ConfigDataMocks {

  describe("PartData") {
    val partDataWithTrimableName = PartData(
      partId = "  　~ wowzers ~　　", // note: a mix of single-byte and multi-byte spaces
      description = None,
      deprecatedTo = None,
      httpSettings = HttpConfigData(
        uri = "",
        method = "get",
        additionalValidStatuses = None,
        httpPoolSize = 20,
        httpConnectionTimeoutInMs = 1000,
        httpSocketTimeoutInMs = 5000,
        httpDefaultEncoding = "UTF-8",
        httpProxy = Some("localhost:666")
      ),
      HystrixConfigData(
        commandKey = "",
        commandGroupKey = "",
        timeoutInMs = 1000,
        threadPoolConfigId = 42L,

        localContentsAsFallback = false
      ),
      cacheGroupNames = Nil,
      ttl = None,
      alertMailData = AlertMailData(
        enabled = false,
        interval = None,
        absoluteThreshold = None,
        percentThreshold = None,
        recipients = None
      ),
      localContentsConfig = LocalContentsConfig(
        enabled = false,
        contents = None
      )
    )

    describe("#toNewHttpPartConfig") {
      it("should trim leading and trailing spaces from the partId") {
        val partConfig = partDataWithTrimableName.toNewHttpPartConfig("chris", SortedSet.empty)
        partConfig.partId should be("~ wowzers ~")
      }
    }

    describe("#toUpdatedHttpPartConfig") {
      it("should trim leading and trailing spaces from the partId") {
        val originalPart = mockHttpPartConfig.copy(hystrixConfig = Some(mockHystrixConfig))
        val updatedPart = partDataWithTrimableName.toUpdatedHttpPartConfig(originalPart, SortedSet.empty)
        updatedPart.partId should be("~ wowzers ~")
      }
    }
  }

}
