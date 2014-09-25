package com.m3.octoparts.logging

import scala.reflect.macros._

private[logging] object LTSVLogWriterMacros {

  type LoggerContext = Context { type PrefixType = LTSVLogWriter }

  /* Info */
  def infoImpl(c: LoggerContext)(pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    val writer = c.prefix.tree
    c.Expr[Unit](q"$writer.info(true, ..$pairs)")
  }

  def infoErrImpl(c: LoggerContext)(error: c.Expr[Throwable], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    val writer = c.prefix.tree
    c.Expr[Unit](q"$writer.info(true, $error, ..$pairs)")
  }

  def infoHostNameImpl(c: LoggerContext)(addHostnameField: c.Expr[Boolean], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    val writer = c.prefix.tree
    c.Expr[Unit](q"if (${writer}.logger.isInfoEnabled) $writer.logger.info($writer.toLtsv(Seq(..$pairs), $addHostnameField))")
  }

  def infoErrHostNameImpl(c: LoggerContext)(addHostnameField: c.Expr[Boolean], error: c.Expr[Throwable], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    val writer = c.prefix.tree
    c.Expr[Unit](q"""if (${writer}.logger.isInfoEnabled) $writer.logger.info($writer.toLtsv(Seq(..$pairs), $addHostnameField), $error)""")
  }

  /* Debug */
  def debugImpl(c: LoggerContext)(pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    val writer = c.prefix.tree
    c.Expr[Unit](q"$writer.debug(true, ..$pairs)")
  }

  def debugErrImpl(c: LoggerContext)(error: c.Expr[Throwable], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    val writer = c.prefix.tree
    c.Expr[Unit](q"$writer.debug(true, $error, ..$pairs)")
  }

  def debugHostNameImpl(c: LoggerContext)(addHostnameField: c.Expr[Boolean], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    val writer = c.prefix.tree
    c.Expr[Unit](q"if (${writer}.logger.isDebugEnabled) $writer.logger.debug($writer.toLtsv(Seq(..$pairs), $addHostnameField))")
  }

  def debugErrHostNameImpl(c: LoggerContext)(addHostnameField: c.Expr[Boolean], error: c.Expr[Throwable], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    val writer = c.prefix.tree
    c.Expr[Unit](q"if (${writer}.logger.isDebugEnabled) $writer.logger.debug($writer.toLtsv(Seq(..$pairs), $addHostnameField), $error)")
  }

  /* Warn */
  def warnImpl(c: LoggerContext)(pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    val writer = c.prefix.tree
    c.Expr[Unit](q"$writer.warn(true, ..$pairs)")
  }

  def warnErrImpl(c: LoggerContext)(error: c.Expr[Throwable], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    val writer = c.prefix.tree
    c.Expr[Unit](q"$writer.warn(true, $error, ..$pairs)")
  }

  def warnHostNameImpl(c: LoggerContext)(addHostnameField: c.Expr[Boolean], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    val writer = c.prefix.tree
    c.Expr[Unit](q"if (${writer}.logger.isWarnEnabled) $writer.logger.warn($writer.toLtsv(Seq(..$pairs), $addHostnameField))")
  }

  def warnErrHostNameImpl(c: LoggerContext)(addHostnameField: c.Expr[Boolean], error: c.Expr[Throwable], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    val writer = c.prefix.tree
    c.Expr[Unit](q"if (${writer}.logger.isWarnEnabled) $writer.logger.warn($writer.toLtsv(Seq(..$pairs), $addHostnameField), $error)")
  }

  /* Error */
  def errorImpl(c: LoggerContext)(pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    val writer = c.prefix.tree
    c.Expr[Unit](q"$writer.error(true, ..$pairs)")
  }

  def errorErrImpl(c: LoggerContext)(error: c.Expr[Throwable], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    val writer = c.prefix.tree
    c.Expr[Unit](q"$writer.error(true, $error, ..$pairs)")
  }

  def errorHostNameImpl(c: LoggerContext)(addHostnameField: c.Expr[Boolean], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    val writer = c.prefix.tree
    c.Expr[Unit](q"if (${writer}.logger.isErrorEnabled) $writer.logger.error($writer.toLtsv(Seq(..$pairs), $addHostnameField))")
  }

  def errorErrHostNameImpl(c: LoggerContext)(addHostnameField: c.Expr[Boolean], error: c.Expr[Throwable], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    val writer = c.prefix.tree
    c.Expr[Unit](q"if (${writer}.logger.isErrorEnabled) $writer.logger.error($writer.toLtsv(Seq(..$pairs), $addHostnameField), $error)")
  }

  /* Trace */
  def traceImpl(c: LoggerContext)(pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    val writer = c.prefix.tree
    c.Expr[Unit](q"$writer.trace(true, ..$pairs)")
  }

  def traceErrImpl(c: LoggerContext)(error: c.Expr[Throwable], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    val writer = c.prefix.tree
    c.Expr[Unit](q"$writer.trace(true, $error, ..$pairs)")
  }

  def traceHostNameImpl(c: LoggerContext)(addHostnameField: c.Expr[Boolean], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    val writer = c.prefix.tree
    c.Expr[Unit](q"if (${writer}.logger.isTraceEnabled) $writer.logger.trace($writer.toLtsv(Seq(..$pairs), $addHostnameField))")
  }

  def traceErrHostNameImpl(c: LoggerContext)(addHostnameField: c.Expr[Boolean], error: c.Expr[Throwable], pairs: c.Expr[(String, Any)]*): c.Expr[Unit] = {
    import c.universe._
    val writer = c.prefix.tree
    c.Expr[Unit](q"if (${writer}.logger.isTraceEnabled) $writer.logger.trace($writer.toLtsv(Seq(..$pairs), $addHostnameField), $error)")
  }

}
