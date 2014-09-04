package com.m3.octoparts.hystrix

import com.netflix.hystrix.{ HystrixCircuitBreaker, HystrixCommandKey, HystrixCommandMetrics }

import scala.collection.convert.Wrappers.JCollectionWrapper

/**
 * A repository to make it easy to query Hystrix metrics information.
 */
trait HystrixCommandMetricsRepository {

  def getAllMetrics: Iterable[HystrixCommandMetrics] =
    JCollectionWrapper(HystrixCommandMetrics.getInstances)

  def getCircuitBreakerForCommand(commandKey: HystrixCommandKey): Option[HystrixCircuitBreaker] =
    Option(HystrixCircuitBreaker.Factory.getInstance(commandKey))

}
