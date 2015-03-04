package com.m3.octoparts.json.format

import java.util.concurrent.TimeUnit

import play.api.libs.json._

import scala.concurrent.duration.FiniteDuration

trait CustomFormatters {

  implicit val durationFormat: Format[FiniteDuration] = Format(Reads {
    case JsNumber(num) => JsSuccess(FiniteDuration(num.toLongExact, TimeUnit.MILLISECONDS))
    case _ => JsError("Invalid num for duration")
  }, Writes {
    o => JsNumber(o.toMillis)
  })
}

object CustomFormatters extends CustomFormatters
