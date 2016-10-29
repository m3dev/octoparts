package com.m3.octoparts.hystrix

trait HystrixHealthReporter {

  /**
   * Get a list of all command keys whose circuit breakers are currently open
   * (i.e. the command is unhealthy)
   */
  def getCommandKeysWithOpenCircuitBreakers: Seq[String]

}

object HystrixHealthReporter
    extends HystrixHealthReporter
    with HystrixCommandMetricsRepository {

  /**
   * Get a list of all command keys whose circuit breakers are currently open
   * (i.e. the command is unhealthy)
   */
  def getCommandKeysWithOpenCircuitBreakers: Seq[String] = {
    (for {
      m <- getAllMetrics
      ck = m.getCommandKey
      cb <- getCircuitBreakerForCommand(ck)
      openCb = cb if cb.isOpen
    } yield ck.name).toSeq
  }

}
