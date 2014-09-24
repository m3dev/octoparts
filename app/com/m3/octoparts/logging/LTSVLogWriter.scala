package com.m3.octoparts.logging

import java.net.InetAddress

import play.api.{ Logger, LoggerLike }
import skinny.util.LTSV

import scala.util.Try

/**
 * Helper trait for writing log messages to a dedicated logger in LTSV format.
 */
trait LTSVLogWriter { writer =>

  private val hostname = Try {
    InetAddress.getLocalHost.getHostName
  } getOrElse "unknown"

  /**
   * The logger to write to.
   */
  protected def logger: LoggerLike

  /* Info */

  /**
   * Write the given key-values as an LTSV info log entry, with the server's hostname added as 'octopartsHost' field.
   */
  @inline final def info(pairs: (String, Any)*): Unit = writer.info(true, pairs: _*)

  /**
   * Write the given key-values and error as an LTSV info log entry, with the server's hostname added as 'octopartsHost' field.
   */
  @inline final def info(error: Throwable, pairs: (String, Any)*): Unit = writer.info(true, error, pairs: _*)

  /**
   * Write the given key-values as an LTSV info log entry.
   */
  @inline final def info(addHostnameField: Boolean, pairs: (String, Any)*): Unit =
    logger.info(toLtsv(pairs, addHostnameField))

  /**
   * Write the given key-values and error as an LTSV info log entry.
   */
  @inline final def info(addHostnameField: Boolean, error: Throwable, pairs: (String, Any)*): Unit =
    logger.info(toLtsv(pairs, addHostnameField), error)

  /* Debug */

  /**
   * Write the given key-values as an LTSV debug log entry, with the server's hostname added as 'octopartsHost' field.
   */
  @inline final def debug(pairs: (String, Any)*): Unit = writer.debug(true, pairs: _*)

  /**
   * Write the given key-values and error as an LTSV debug log entry, with the server's hostname added as 'octopartsHost' field.
   */
  @inline final def debug(error: Throwable, pairs: (String, Any)*): Unit = writer.debug(true, error, pairs: _*)

  /**
   * Write the given key-values as an LTSV debug log entry.
   */
  @inline final def debug(addHostnameField: Boolean, pairs: (String, Any)*): Unit =
    logger.debug(toLtsv(pairs, addHostnameField))

  /**
   * Write the given key-values and error as an LTSV debug log entry.
   */
  @inline final def debug(addHostnameField: Boolean, error: Throwable, pairs: (String, Any)*): Unit =
    logger.debug(toLtsv(pairs, addHostnameField), error)

  /* Warn */

  /**
   * Write the given key-values as an LTSV warn log entry, with the server's hostname added as 'octopartsHost' field.
   */
  @inline final def warn(pairs: (String, Any)*): Unit = writer.warn(true, pairs: _*)

  /**
   * Write the given key-values and error as an LTSV warn log entry, with the server's hostname added as 'octopartsHost' field.
   */
  @inline final def warn(error: Throwable, pairs: (String, Any)*): Unit = writer.warn(true, error, pairs: _*)

  /**
   * Write the given key-values as an LTSV warn log entry.
   */
  @inline final def warn(addHostnameField: Boolean, pairs: (String, Any)*): Unit =
    logger.warn(toLtsv(pairs, addHostnameField))

  /**
   * Write the given key-values and error as an LTSV warn log entry.
   */
  @inline final def warn(addHostnameField: Boolean, error: Throwable, pairs: (String, Any)*): Unit =
    logger.warn(toLtsv(pairs, addHostnameField), error)

  /* Error */

  /**
   * Write the given key-values as an LTSV error log entry, with the server's hostname added as 'octopartsHost' field.
   */
  @inline final def error(pairs: (String, Any)*): Unit = writer.error(true, pairs: _*)

  /**
   * Write the given key-values and error as an LTSV error log entry, with the server's hostname added as 'octopartsHost' field.
   */
  @inline final def error(error: Throwable, pairs: (String, Any)*): Unit = writer.error(true, error, pairs: _*)

  /**
   * Write the given key-values as an LTSV error log entry.
   */
  @inline final def error(addHostnameField: Boolean, pairs: (String, Any)*): Unit =
    logger.error(toLtsv(pairs, addHostnameField))

  /**
   * Write the given key-values and error as an LTSV error log entry.
   */
  @inline final def error(addHostnameField: Boolean, error: Throwable, pairs: (String, Any)*): Unit =
    logger.error(toLtsv(pairs, addHostnameField), error)

  /* Trace */

  /**
   * Write the given key-values as an LTSV error log entry, with the server's hostname added as 'octopartsHost' field.
   */
  @inline final def trace(pairs: (String, Any)*): Unit = writer.trace(true, pairs: _*)

  /**
   * Write the given key-values and error as an LTSV error log entry, with the server's hostname added as 'octopartsHost' field.
   */
  @inline final def trace(error: Throwable, pairs: (String, Any)*): Unit = writer.trace(true, error, pairs: _*)

  /**
   * Write the given key-values as an LTSV error log entry.
   */
  @inline final def trace(addHostnameField: Boolean, pairs: (String, Any)*): Unit =
    logger.trace(toLtsv(pairs, addHostnameField))
  /**
   * Write the given key-values and error as an LTSV error log entry.
   */
  @inline final def trace(addHostnameField: Boolean, error: Throwable, pairs: (String, Any)*): Unit =
    logger.trace(toLtsv(pairs, addHostnameField), error)

  @inline private def toLtsv(pairs: Seq[(String, Any)], addHostnameField: Boolean): String = {
    val logMsg = withHostnameFieldIfNeeded(pairs, addHostnameField).map {
      case (k, v) => (k, String.valueOf(v))
    }
    LTSV.dump(logMsg: _*)
  }

  @inline private def withHostnameFieldIfNeeded(pairs: Seq[(String, Any)], addHostnameField: Boolean) = {
    if (addHostnameField)
      pairs :+ ("octopartsHost" -> hostname)
    else
      pairs
  }

}

/**
 * LTSVLogWriter that writes using the default Play Logger
 */
object LTSVLogWriter extends LTSVLogWriter {
  val logger = Logger
}