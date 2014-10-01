package com.m3.octoparts.hystrix

import com.netflix.hystrix.{ HystrixCommandKey, HystrixCommandProperties }
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy

/**
 * Custom [[HystrixPropertiesStrategy]] implementation where  getCommandPropertiesCacheKey is overridden
 * to return null so that [[com.netflix.hystrix.strategy.properties.HystrixPropertiesFactory]] will always
 * generate a new [[HystrixCommandProperties]] for each new [[com.netflix.hystrix.HystrixCommand]] instantiation
 */
object CachelessHystrixPropertiesStrategy extends HystrixPropertiesStrategy {
  override def getCommandPropertiesCacheKey(commandKey: HystrixCommandKey, builder: HystrixCommandProperties.Setter): String = null
}
