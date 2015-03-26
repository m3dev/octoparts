package com.m3.octoparts.hystrix

import com.m3.octoparts.Global
import com.netflix.hystrix.strategy.HystrixPlugins
import com.netflix.hystrix.strategy.properties.HystrixPropertiesFactory
import com.netflix.hystrix.{ HystrixCommandProperties, HystrixCommandKey }
import org.scalatest.{ Matchers, FunSpec }

class KeyAndBuilderValuesHystrixPropertiesStrategySpec extends FunSpec with Matchers {

  private val subject = new KeyAndBuilderValuesHystrixPropertiesStrategy
  private val commandKey = HystrixCommandKey.Factory.asKey("hello")

  describe("getCommandPropertiesCacheKey") {
    it("should return a combination of the commandKey name and commandProps JSON value") {
      val r1 = subject.getCommandPropertiesCacheKey(commandKey, HystrixCommandProperties.Setter())
      r1 should be("""hello-{}""")
      val r2 = subject.getCommandPropertiesCacheKey(commandKey, HystrixCommandProperties.Setter().withCircuitBreakerEnabled(true))
      r2 should be("""hello-{"circuitBreakerEnabled":true}""")
    }
  }

  describe("after registering with HystrixPlugins") {
    it("should allow HystrixPropertiesFactory.getCommandProperties to instantiate different HystrixCommandProperties for the same command key") {
      Global.setHystrixPropertiesStrategy()
      HystrixPlugins.getInstance().getPropertiesStrategy.getClass shouldBe subject.getClass

      val properties1 = HystrixPropertiesFactory.getCommandProperties(
        commandKey,
        HystrixCommandProperties.Setter().withExecutionTimeoutInMilliseconds(300))
      val properties2 = HystrixPropertiesFactory.getCommandProperties(
        commandKey,
        HystrixCommandProperties.Setter().withExecutionTimeoutInMilliseconds(600))
      properties1.executionTimeoutInMilliseconds.get should be(300)
      properties2.executionTimeoutInMilliseconds.get should be(600)
    }
  }

}
