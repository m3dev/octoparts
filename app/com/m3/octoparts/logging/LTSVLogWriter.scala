package com.m3.octoparts.logging

import java.net.InetAddress

import skinny.logging.Logger
import skinny.util.LTSV

import scala.util.Try

/**
 * Helper trait for writing log messages to a dedicated logger in LTSV format.
 */
trait LTSVLogWriter {

  private val hostname = Try {
    InetAddress.getLocalHost.getHostName
  } getOrElse "unknown"

  /**
   * The Skinny logger to write to.
   */
  def logger: Logger

  /**
   * Write the given key-values as an LTSV log entry, with the server's hostname added as 'octopartsHost' field.
   * @param pairs key-value pairs
   */
  def info(pairs: (String, Any)*): Unit = info(true, pairs: _*)

  /**
   * Write the given key-values as an LTSV log entry.
   * @param addHostnameField Include the server's hostname as an 'octopartsHost' field
   * @param pairs key-value pairs
   */
  def info(addHostnameField: Boolean, pairs: (String, Any)*): Unit =
    logger.info(toLtsv(pairs, addHostnameField))

  private def toLtsv(pairs: Seq[(String, Any)], addHostnameField: Boolean): String = {
    val logMsg = withHostnameFieldIfNeeded(pairs, addHostnameField).map {
      case (k, v) => (k, String.valueOf(v))
    }
    LTSV.dump(logMsg: _*)
  }

  private def withHostnameFieldIfNeeded(pairs: Seq[(String, Any)], addHostnameField: Boolean) = {
    if (addHostnameField)
      pairs :+ ("octopartsHost" -> hostname)
    else
      pairs
  }

}
