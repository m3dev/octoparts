package com.m3.octoparts.model.config

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
      val jsonModel = HttpPartConfig.toJsonModel(mockHttpPartConfig.copy(hystrixConfig = Some(mockHystrixConfig)))
    }

  }

}
