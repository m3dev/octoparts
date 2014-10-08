package com.m3.octoparts.hystrix

import com.netflix.hystrix.strategy.HystrixPlugins
import com.netflix.hystrix.strategy.properties.HystrixPropertiesFactory
import com.netflix.hystrix.{ HystrixCommandProperties, HystrixCommandKey }
import org.scalatest.{ Matchers, FunSpec }

class KeyAndBuilderValuesHystrixPropertiesStrategySpec extends FunSpec with Matchers {

  val subject = new KeyAndBuilderValuesHystrixPropertiesStrategy
  val commandKey = HystrixCommandKey.Factory.asKey("hello")
  val commandProps = HystrixCommandProperties.Setter()
  describe("getCommandPropertiesCacheKey") {
    it("should return a combination of the commandKey name and commandProps JSON value") {
      val r1 = subject.getCommandPropertiesCacheKey(commandKey, commandProps)
      r1 should be("""hello-{}""")
      val r2 = subject.getCommandPropertiesCacheKey(commandKey, HystrixCommandProperties.Setter().withExecutionIsolationThreadTimeoutInMilliseconds(100))
      r2 should be("""hello-{"executionIsolationThreadTimeoutInMilliseconds":100}""")
    }
  }

  // The following works if this test is run by itself, but
  describe("after registering with HystrixPlugins") {

    it("should allow HystrixPropertiesFactory.getCommandProperties to instantiate different HystrixCommandProperties for the same command key") {
      if (HystrixPlugins.getInstance().getPropertiesStrategy.getClass != subject.getClass) {
        fail("HystrixPlugins.getPropertiesStrategy did not return KeyAndBuilderValuesHystrixPropertiesStrategy")
      }
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
