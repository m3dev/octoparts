package com.m3.octoparts.logging

import scala.reflect.macros._

private[logging] object LTSVLogWriterMacros {

  type LoggerContext = Context { type PrefixType = LTSVLogWriter }

  /* Info */
  def infoImpl(c: LoggerContext)(pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    infoHostNameImpl(c)(c.Expr[Boolean](q"true"), pairs: _*)
  }

  def infoErrImpl(c: LoggerContext)(error: c.Expr[Throwable], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    infoErrHostNameImpl(c)(c.Expr[Boolean](q"true"), error, pairs: _*)
  }

  def infoHostNameImpl(c: LoggerContext)(addHostnameField: c.Expr[Boolean], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] =
    ltsvLogAtLevelIfEnabled(c)("info", addHostnameField, pairs: _*)

  def infoErrHostNameImpl(c: LoggerContext)(addHostnameField: c.Expr[Boolean], error: c.Expr[Throwable], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] =
    ltsvErrLogAtLevelIfEnabled(c)("info", addHostnameField, error, pairs: _*)

  /* Debug */
  def debugImpl(c: LoggerContext)(pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    debugHostNameImpl(c)(c.Expr[Boolean](q"true"), pairs: _*)
  }

  def debugErrImpl(c: LoggerContext)(error: c.Expr[Throwable], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    val writer = c.prefix.tree
    debugErrHostNameImpl(c)(c.Expr[Boolean](q"true"), error, pairs: _*)
  }

  def debugHostNameImpl(c: LoggerContext)(addHostnameField: c.Expr[Boolean], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] =
    ltsvLogAtLevelIfEnabled(c)("debug", addHostnameField, pairs: _*)

  def debugErrHostNameImpl(c: LoggerContext)(addHostnameField: c.Expr[Boolean], error: c.Expr[Throwable], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] =
    ltsvErrLogAtLevelIfEnabled(c)("debug", addHostnameField, error, pairs: _*)

  /* Warn */
  def warnImpl(c: LoggerContext)(pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    warnHostNameImpl(c)(c.Expr[Boolean](q"true"), pairs: _*)
  }

  def warnErrImpl(c: LoggerContext)(error: c.Expr[Throwable], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    val writer = c.prefix.tree
    warnErrHostNameImpl(c)(c.Expr[Boolean](q"true"), error, pairs: _*)
  }

  def warnHostNameImpl(c: LoggerContext)(addHostnameField: c.Expr[Boolean], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] =
    ltsvLogAtLevelIfEnabled(c)("warn", addHostnameField, pairs: _*)

  def warnErrHostNameImpl(c: LoggerContext)(addHostnameField: c.Expr[Boolean], error: c.Expr[Throwable], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] =
    ltsvErrLogAtLevelIfEnabled(c)("warn", addHostnameField, error, pairs: _*)

  /* Error */
  def errorImpl(c: LoggerContext)(pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    errorHostNameImpl(c)(c.Expr[Boolean](q"true"), pairs: _*)
  }

  def errorErrImpl(c: LoggerContext)(error: c.Expr[Throwable], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    errorErrHostNameImpl(c)(c.Expr[Boolean](q"true"), error, pairs: _*)
  }

  def errorHostNameImpl(c: LoggerContext)(addHostnameField: c.Expr[Boolean], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] =
    ltsvLogAtLevelIfEnabled(c)("error", addHostnameField, pairs: _*)

  def errorErrHostNameImpl(c: LoggerContext)(addHostnameField: c.Expr[Boolean], error: c.Expr[Throwable], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] =
    ltsvErrLogAtLevelIfEnabled(c)("error", addHostnameField, error, pairs: _*)

  /* Trace */
  def traceImpl(c: LoggerContext)(pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    traceHostNameImpl(c)(c.Expr[Boolean](q"true"), pairs: _*)
  }

  def traceErrImpl(c: LoggerContext)(error: c.Expr[Throwable], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    val writer = c.prefix.tree
    traceErrHostNameImpl(c)(c.Expr[Boolean](q"true"), error, pairs: _*)
  }

  def traceHostNameImpl(c: LoggerContext)(addHostnameField: c.Expr[Boolean], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] =
    ltsvLogAtLevelIfEnabled(c)("trace", addHostnameField, pairs: _*)

  def traceErrHostNameImpl(c: LoggerContext)(addHostnameField: c.Expr[Boolean], error: c.Expr[Throwable], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] =
    ltsvErrLogAtLevelIfEnabled(c)("trace", addHostnameField, error, pairs: _*)

  private def ltsvLogAtLevelIfEnabled(c: LoggerContext)(level: String, addHostnameField: c.Expr[Boolean], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    val isLevelEnabled = newTermName(s"is${level.toLowerCase.capitalize}Enabled")
    val logLevel = newTermName(s"${level.toLowerCase}")
    val writer = c.prefix.tree
    c.Expr[Unit](q"if (${writer}.logger.$isLevelEnabled) $writer.logger.$logLevel($writer.toLtsv(Seq(..$pairs), $addHostnameField))")
  }

  private def ltsvErrLogAtLevelIfEnabled(c: LoggerContext)(level: String, addHostnameField: c.Expr[Boolean], error: c.Expr[Throwable], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    val isLevelEnabled = newTermName(s"is${level.toLowerCase.capitalize}Enabled")
    val logLevel = newTermName(s"${level.toLowerCase}")
    val writer = c.prefix.tree
    c.Expr[Unit](q"if (${writer}.logger.$isLevelEnabled) $writer.logger.$logLevel($writer.toLtsv(Seq(..$pairs), $addHostnameField), $error)")
  }

}
