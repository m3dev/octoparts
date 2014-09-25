package com.m3.octoparts.logging

import java.net.InetAddress

import com.github.seratch.ltsv4s.LTSV
import org.slf4j.{ LoggerFactory, Logger => Slf4jLogger }

import scala.util.Try

/**
 * Helper trait for writing log messages to a dedicated logger in LTSV format.
 *
 * The logging methods in this trait are performant because the check-enabled-idiom
 * is applied using macros. For example,
 *
 * {{{
 * logger.info("message" -> s"$expensiveMessage"*)
 * }}}
 *
 * gets expanded at compile-time to
 *
 * {{{
 * if (logger.isDebugEnabled) logger.info(toLtsv("message" -> s"$expensiveMessage"*))
 * }}}
 */
trait LTSVLogWriter { writer =>

  import scala.language.experimental.macros

  private val hostname = Try {
    InetAddress.getLocalHost.getHostName
  } getOrElse "unknown"

  /**
   * The logger to write to.
   */
  def logger: Slf4jLogger

  /* Info */

  /**
   * Write the given key-values as an LTSV info log entry, with the server's hostname added as 'octopartsHost' field.
   */
  @inline final def info(pairs: (String, Any)*): Unit = macro LTSVLogWriterMacros.infoImpl
  /**
   * Write the given key-values and error as an LTSV info log entry, with the server's hostname added as 'octopartsHost' field.
   */
  @inline final def info(error: Throwable, pairs: (String, Any)*): Unit = macro LTSVLogWriterMacros.infoErrImpl

  /**
   * Write the given key-values as an LTSV info log entry.
   */
  @inline final def info(addHostnameField: Boolean, pairs: (String, Any)*): Unit = macro LTSVLogWriterMacros.infoHostNameImpl

  /**
   * Write the given key-values and error as an LTSV info log entry.
   */
  @inline final def info(addHostnameField: Boolean, error: Throwable, pairs: (String, Any)*): Unit = macro LTSVLogWriterMacros.infoErrHostNameImpl

  /* Debug */

  /**
   * Write the given key-values as an LTSV debug log entry, with the server's hostname added as 'octopartsHost' field.
   */
  @inline final def debug(pairs: (String, Any)*): Unit = macro LTSVLogWriterMacros.debugImpl

  /**
   * Write the given key-values and error as an LTSV debug log entry, with the server's hostname added as 'octopartsHost' field.
   */
  @inline final def debug(error: Throwable, pairs: (String, Any)*): Unit = macro LTSVLogWriterMacros.debugErrImpl

  /**
   * Write the given key-values as an LTSV debug log entry.
   */
  @inline final def debug(addHostnameField: Boolean, pairs: (String, Any)*): Unit = macro LTSVLogWriterMacros.debugHostNameImpl

  /**
   * Write the given key-values and error as an LTSV debug log entry.
   */
  @inline final def debug(addHostnameField: Boolean, error: Throwable, pairs: (String, Any)*): Unit = macro LTSVLogWriterMacros.debugErrHostNameImpl

  /* Warn */

  /**
   * Write the given key-values as an LTSV warn log entry, with the server's hostname added as 'octopartsHost' field.
   */
  @inline final def warn(pairs: (String, Any)*): Unit = macro LTSVLogWriterMacros.warnImpl

  /**
   * Write the given key-values and error as an LTSV warn log entry, with the server's hostname added as 'octopartsHost' field.
   */
  @inline final def warn(error: Throwable, pairs: (String, Any)*): Unit = macro LTSVLogWriterMacros.warnErrImpl

  /**
   * Write the given key-values as an LTSV warn log entry.
   */
  @inline final def warn(addHostnameField: Boolean, pairs: (String, Any)*): Unit = macro LTSVLogWriterMacros.warnHostNameImpl

  /**
   * Write the given key-values and error as an LTSV warn log entry.
   */
  @inline final def warn(addHostnameField: Boolean, error: Throwable, pairs: (String, Any)*): Unit = macro LTSVLogWriterMacros.warnErrHostNameImpl

  /* Error */

  /**
   * Write the given key-values as an LTSV error log entry, with the server's hostname added as 'octopartsHost' field.
   */
  @inline final def error(pairs: (String, Any)*): Unit = macro LTSVLogWriterMacros.errorImpl

  /**
   * Write the given key-values and error as an LTSV error log entry, with the server's hostname added as 'octopartsHost' field.
   */
  @inline final def error(error: Throwable, pairs: (String, Any)*): Unit = macro LTSVLogWriterMacros.errorErrImpl

  /**
   * Write the given key-values as an LTSV error log entry.
   */
  @inline final def error(addHostnameField: Boolean, pairs: (String, Any)*): Unit = macro LTSVLogWriterMacros.errorHostNameImpl

  /**
   * Write the given key-values and error as an LTSV error log entry.
   */
  @inline final def error(addHostnameField: Boolean, error: Throwable, pairs: (String, Any)*): Unit = macro LTSVLogWriterMacros.errorErrHostNameImpl

  /* Trace */

  /**
   * Write the given key-values as an LTSV trace log entry, with the server's hostname added as 'octopartsHost' field.
   */
  @inline final def trace(pairs: (String, Any)*): Unit = macro LTSVLogWriterMacros.traceImpl

  /**
   * Write the given key-values and error as an LTSV trace log entry, with the server's hostname added as 'octopartsHost' field.
   */
  @inline final def trace(error: Throwable, pairs: (String, Any)*): Unit = macro LTSVLogWriterMacros.traceErrImpl

  /**
   * Write the given key-values as an LTSV trace log entry.
   */
  @inline final def trace(addHostnameField: Boolean, pairs: (String, Any)*): Unit = macro LTSVLogWriterMacros.traceHostNameImpl
  /**
   * Write the given key-values and error as an LTSV trace log entry.
   */
  @inline final def trace(addHostnameField: Boolean, error: Throwable, pairs: (String, Any)*): Unit = macro LTSVLogWriterMacros.traceErrHostNameImpl

  @inline def toLtsv(pairs: Seq[(String, Any)], addHostnameField: Boolean): String = {
    val logMsg = withHostnameFieldIfNeeded(pairs, addHostnameField).map {
      case (k, v) => (k, String.valueOf(v))
    }
    LTSV.dump(logMsg: _*)
  }

  @inline def withHostnameFieldIfNeeded(pairs: Seq[(String, Any)], addHostnameField: Boolean) = {
    if (addHostnameField)
      pairs :+ ("octopartsHost" -> hostname)
    else
      pairs
  }

}

object LTSVLogWriter extends LTSVLogWriter {
  val logger = LoggerFactory.getLogger("application")
}