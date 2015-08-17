package com.m3.octoparts.json.format

import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

import play.api.libs.json._

import scala.concurrent.duration.FiniteDuration
import scala.util.{ Failure, Success, Try }

trait CustomFormatters {

  implicit val charsetFormat: Format[Charset] = Format(Reads {
    case JsString(name) => Try {
      Charset.forName(name)
    } match {
      case Success(charset) => JsSuccess(charset)
      case Failure(err) => JsError(err.toString)
    }
    case _ => JsError("Invalid charset")
  }, Writes {
    o => JsString(o.name())
  })

  implicit val durationFormat: Format[FiniteDuration] = Format(Reads {
    case JsNumber(num) => JsSuccess(FiniteDuration(num.toLongExact, TimeUnit.MILLISECONDS))
    case _ => JsError("Invalid num for duration")
  }, Writes {
    o => JsNumber(o.toMillis)
  })
}

object CustomFormatters extends CustomFormatters
