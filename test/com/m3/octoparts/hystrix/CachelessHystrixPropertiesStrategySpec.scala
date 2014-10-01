package com.m3.octoparts.hystrix

import com.netflix.hystrix.strategy.HystrixPlugins
import com.netflix.hystrix.strategy.properties.HystrixPropertiesFactory
import com.netflix.hystrix.{ HystrixCommandProperties, HystrixCommandKey }
import org.scalatest.{ Matchers, FunSpec }

class CachelessHystrixPropertiesStrategySpec extends FunSpec with Matchers {

  val commandKey = HystrixCommandKey.Factory.asKey("hello")
  val commandProps = HystrixCommandProperties.Setter()
  describe("getCommandPropertiesCacheKey") {
    it("should return null") {
      val r = CachelessHystrixPropertiesStrategy.getCommandPropertiesCacheKey(commandKey, commandProps)
      r should be(null)
    }
  }

  describe("after registering with HystrixPlugins") {

    it("should allow HystrixPropertiesFactory.getCommandProperties to instantiate different HystrixCommandProperties for the same command key") {
      HystrixPlugins.getInstance().registerPropertiesStrategy(CachelessHystrixPropertiesStrategy)
      val properties1 = HystrixPropertiesFactory.getCommandProperties(
        commandKey,
        HystrixCommandProperties.Setter().withExecutionIsolationThreadTimeoutInMilliseconds(300))
      val properties2 = HystrixPropertiesFactory.getCommandProperties(
        commandKey,
        HystrixCommandProperties.Setter().withExecutionIsolationThreadTimeoutInMilliseconds(600))
      properties1.executionIsolationThreadTimeoutInMilliseconds.get should be(300)
      properties2.executionIsolationThreadTimeoutInMilliseconds.get should be(600)
    }
  }

}
