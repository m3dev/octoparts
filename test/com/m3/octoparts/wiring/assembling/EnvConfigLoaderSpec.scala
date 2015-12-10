package com.m3.octoparts.wiring.assembling

import com.m3.octoparts.support.PlayAppSupport
import org.scalatest.{ Matchers, FunSpec }

class EnvConfigLoaderSpec extends FunSpec with Matchers with PlayAppSupport with EnvConfigLoader {

  describe("#configFileResolvable") {

    it("should return true for config files that are in the class path") {
      configFileResolvable("application.conf", context.environment) shouldBe true
      configFileResolvable("application.ci.conf", context.environment) shouldBe true
    }

    it("should return false for config files that are not in the class path") {
      configFileResolvable("application.st00pid.conf", context.environment) shouldBe false
    }

  }

}