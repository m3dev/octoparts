package com.m3.octoparts.hystrix

import com.m3.octoparts.logging.LTSVLogWriter
import skinny.logging.Logger

/**
 * Helper for outputting Hystrix metrics logs in LTSV format.
 * These logs are written to a separate log file for tailing by fluentd.
 */
object HystrixMetricsLogger extends HystrixCommandMetricsRepository with LTSVLogWriter {

  val logger = Logger("HystrixMetrics")

  /**
   * Collect metrics on all registered Hystrix commands and write them to a log file.
   * Writes one log per Hystrix command (i.e. one per partId).
   */
  def logHystrixMetrics(): Unit = {
    for {
      m <- getAllMetrics
      ck = m.getCommandKey
    } log(
      ck.name,
      getCircuitBreakerForCommand(ck).exists(_.isOpen),
      m.getHealthCounts.getErrorCount,
      m.getHealthCounts.getErrorPercentage,
      m.getExecutionTimeMean,
      m.getExecutionTimePercentile(50),
      m.getExecutionTimePercentile(95),
      m.getExecutionTimePercentile(99),
      m.getExecutionTimePercentile(99.8)
    )
  }

  private def log(
    commandName: String,
    circuitOpen: Boolean,
    errorCount: Long,
    errorPercentage: Int,
    execTimeMsMean: Int,
    execTimeMsMedian: Int,
    execTimeMs95: Int,
    execTimeMs99: Int,
    execTimeMs998: Int): Unit = {

    info(
      "commandName" -> commandName,
      "circuitOpen" -> circuitOpen,
      "errorCount" -> errorCount,
      "errorPercentage" -> errorPercentage,
      "execTimeMsMean" -> execTimeMsMean,
      "execTimeMsMedian" -> execTimeMsMedian,
      "execTimeMs95" -> execTimeMs95,
      "execTimeMs99" -> execTimeMs99,
      "execTimeMs998" -> execTimeMs998
    )
  }

}
