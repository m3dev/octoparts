package controllers

import com.m3.octoparts.model.config.{ PartParam, CacheGroup }
import com.m3.octoparts.support.mocks.ConfigDataMocks
import controllers.AdminForms.{ LocalContentsConfig, PartData }
import org.scalatest.{ FunSpec, Matchers }

class AdminFormsSpec extends FunSpec with Matchers with ConfigDataMocks {

  describe("PartData") {
    val partDataWithTrimableName = PartData(
      partId = "  　~ wowzers ~　　", // note: a mix of single-byte and multi-byte spaces
      description = None,
      deprecatedTo = None,
      uri = "",
      method = "get",
      additionalValidStatuses = None,
      commandKey = "",
      commandGroupKey = "",
      timeoutInMs = 1000L,
      threadPoolConfigId = 42L,
      cacheGroupNames = Nil,
      ttl = None,
      alertMailsEnabled = false,
      alertInterval = None,
      alertAbsoluteThreshold = None,
      alertPercentThreshold = None,
      alertMailRecipients = None,
      localContentsConfig = LocalContentsConfig(enabled = false, contents = None))

    describe("#toNewHttpPartConfig") {
      it("should trim leading and trailing spaces from the partId") {
        val partConfig = partDataWithTrimableName.toNewHttpPartConfig("chris", Set.empty[CacheGroup])
        partConfig.partId should be("~ wowzers ~")
      }
    }

    describe("#toUpdatedHttpPartConfig") {
      it("should trim leading and trailing spaces from the partId") {
        val originalPart = mockHttpPartConfig.copy(hystrixConfig = Some(mockHystrixConfig))
        val updatedPart = partDataWithTrimableName.toUpdatedHttpPartConfig(originalPart, Set.empty[PartParam], Set.empty[CacheGroup])
        updatedPart.partId should be("~ wowzers ~")
      }
    }
  }

}
