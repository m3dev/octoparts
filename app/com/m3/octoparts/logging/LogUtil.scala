package com.m3.octoparts.logging

import org.apache.commons.lang.StringUtils
import play.api.Logger

trait LogUtil {

  protected def truncateValue(value: Any, max: Int = 20): String = {
    Option(value).map(_.toString).fold("") { s =>
      if (Logger.isTraceEnabled) s
      else StringUtils.abbreviate(s, 0, max)
    }
  }
}
