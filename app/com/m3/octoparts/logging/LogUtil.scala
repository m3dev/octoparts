package com.m3.octoparts.logging

import org.apache.commons.lang.StringUtils
import play.api.{ LoggerLike, Logger }

trait LogUtil {

  // Override as needed
  def logger: LoggerLike = Logger

  protected def truncateValue(value: Any, max: Int = 20): String = {
    Option(value).map(_.toString).fold("") { s =>
      if (logger.isTraceEnabled) s
      else StringUtils.abbreviate(s, 0, max)
    }
  }
}
