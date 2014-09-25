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
    ltsvErrLogAtLevelIfEnabled(c)("info", addHostnameField, None, pairs: _*)

  def infoErrHostNameImpl(c: LoggerContext)(addHostnameField: c.Expr[Boolean], error: c.Expr[Throwable], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] =
    ltsvErrLogAtLevelIfEnabled(c)("info", addHostnameField, Some(error), pairs: _*)

  /* Debug */
  def debugImpl(c: LoggerContext)(pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    debugHostNameImpl(c)(c.Expr[Boolean](q"true"), pairs: _*)
  }

  def debugErrImpl(c: LoggerContext)(error: c.Expr[Throwable], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    debugErrHostNameImpl(c)(c.Expr[Boolean](q"true"), error, pairs: _*)
  }

  def debugHostNameImpl(c: LoggerContext)(addHostnameField: c.Expr[Boolean], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] =
    ltsvErrLogAtLevelIfEnabled(c)("debug", addHostnameField, None, pairs: _*)

  def debugErrHostNameImpl(c: LoggerContext)(addHostnameField: c.Expr[Boolean], error: c.Expr[Throwable], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] =
    ltsvErrLogAtLevelIfEnabled(c)("debug", addHostnameField, Some(error), pairs: _*)

  /* Warn */
  def warnImpl(c: LoggerContext)(pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    warnHostNameImpl(c)(c.Expr[Boolean](q"true"), pairs: _*)
  }

  def warnErrImpl(c: LoggerContext)(error: c.Expr[Throwable], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    warnErrHostNameImpl(c)(c.Expr[Boolean](q"true"), error, pairs: _*)
  }

  def warnHostNameImpl(c: LoggerContext)(addHostnameField: c.Expr[Boolean], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] =
    ltsvErrLogAtLevelIfEnabled(c)("warn", addHostnameField, None, pairs: _*)

  def warnErrHostNameImpl(c: LoggerContext)(addHostnameField: c.Expr[Boolean], error: c.Expr[Throwable], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] =
    ltsvErrLogAtLevelIfEnabled(c)("warn", addHostnameField, Some(error), pairs: _*)

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
    ltsvErrLogAtLevelIfEnabled(c)("error", addHostnameField, None, pairs: _*)

  def errorErrHostNameImpl(c: LoggerContext)(addHostnameField: c.Expr[Boolean], error: c.Expr[Throwable], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] =
    ltsvErrLogAtLevelIfEnabled(c)("error", addHostnameField, Some(error), pairs: _*)

  /* Trace */
  def traceImpl(c: LoggerContext)(pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    traceHostNameImpl(c)(c.Expr[Boolean](q"true"), pairs: _*)
  }

  def traceErrImpl(c: LoggerContext)(error: c.Expr[Throwable], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    traceErrHostNameImpl(c)(c.Expr[Boolean](q"true"), error, pairs: _*)
  }

  def traceHostNameImpl(c: LoggerContext)(addHostnameField: c.Expr[Boolean], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] =
    ltsvErrLogAtLevelIfEnabled(c)("trace", addHostnameField, None, pairs: _*)

  def traceErrHostNameImpl(c: LoggerContext)(addHostnameField: c.Expr[Boolean], error: c.Expr[Throwable], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] =
    ltsvErrLogAtLevelIfEnabled(c)("trace", addHostnameField, Some(error), pairs: _*)

  private def ltsvErrLogAtLevelIfEnabled(c: LoggerContext)(level: String, addHostnameField: c.Expr[Boolean], error: Option[c.Expr[Throwable]], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    val isLevelEnabled = newTermName(s"is${level.toLowerCase.capitalize}Enabled")
    val logLevel = newTermName(level.toLowerCase)
    val writer = c.prefix.tree
    val tree = error match {
      case Some(err) => q"if (${writer}.logger.$isLevelEnabled) $writer.logger.$logLevel($writer.toLtsv(Seq(..$pairs), $addHostnameField), $err)"
      case None => q"if (${writer}.logger.$isLevelEnabled) $writer.logger.$logLevel($writer.toLtsv(Seq(..$pairs), $addHostnameField))"
    }
    c.Expr[Unit](tree)
  }

}
