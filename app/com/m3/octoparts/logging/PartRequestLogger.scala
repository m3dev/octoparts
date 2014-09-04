package com.m3.octoparts.logging

import skinny.logging.Logger

/**
 * Helper for logging the results of part requests in LTSV format.
 * These logs are written to a separate log file for tailing by fluentd.
 */
trait PartRequestLogger {

  def logSuccess(partId: String, parentRequestId: String, serviceId: Option[String], cacheHit: Boolean, responseMs: Long): Unit

  def logFailure(partId: String, parentRequestId: String, serviceId: Option[String], statusCode: Option[Int]): Unit

  def logTimeout(partId: String, parentRequestId: String, serviceId: Option[String], timeoutMs: Long): Unit

}

object PartRequestLogger extends PartRequestLogger with LTSVLogWriter {

  val logger = Logger("PartRequests")

  def logSuccess(partId: String, parentRequestId: String, serviceId: Option[String], cacheHit: Boolean, responseMs: Long): Unit = {
    val hitOrMiss = if (cacheHit) "hit" else "miss"
    info(
      "partId" -> partId,
      "result" -> s"success_cache_$hitOrMiss",
      "parentRequestId" -> parentRequestId,
      "serviceId" -> serviceId.getOrElse(""),
      "responseMs" -> responseMs
    )
  }

  def logFailure(partId: String, parentRequestId: String, serviceId: Option[String], statusCode: Option[Int]): Unit = {
    info(
      "partId" -> partId,
      "result" -> "failure",
      "parentRequestId" -> parentRequestId,
      "serviceId" -> serviceId.getOrElse(""),
      "statusCode" -> statusCode.fold("")(_.toString)
    )
  }

  def logTimeout(partId: String, parentRequestId: String, serviceId: Option[String], timeoutMs: Long): Unit = {
    info(
      "partId" -> partId,
      "result" -> "timeout",
      "parentRequestId" -> parentRequestId,
      "serviceId" -> serviceId.getOrElse(""),
      "timeoutMs" -> timeoutMs
    )
  }
}
