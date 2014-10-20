package com.m3.octoparts.logging

import org.apache.commons.lang.StringUtils
import play.api.{ LoggerLike, Logger }

import scala.concurrent.duration._

trait LogUtil {

  // Override as needed
  def logger: LoggerLike = Logger

  protected def truncateValue(value: Any, max: Int = 20): String = {
    Option(value).map(_.toString).fold("") { s =>
      if (logger.isTraceEnabled) s
      else StringUtils.abbreviate(s, 0, max)
    }
  }

  protected def toRelevantUnit(duration: Duration): Duration = duration match {
    case Duration.Inf => Duration.Inf
    case nano if nano < 1.micro => duration.toNanos.nanos
    case micro if micro < 1.milli => duration.toMicros.micros
    case milli if milli < 1.second => duration.toMillis.millis
    case second if second < 1.minute => duration.toSeconds.seconds
    case minute if minute < 1.hour => duration.toMinutes.minutes
    case _ => duration.toHours.hours
  }
}
