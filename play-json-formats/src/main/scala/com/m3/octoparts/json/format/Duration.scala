package com.m3.octoparts.json.format

import java.util.concurrent.TimeUnit

import play.api.libs.json._

import scala.concurrent.duration.{ Duration => ScalaDuration }

object Duration {

  implicit val format = new Format[ScalaDuration] {
    def writes(o: ScalaDuration): JsValue = JsNumber(o.toMillis)
    def reads(json: JsValue): JsResult[ScalaDuration] = json match {
      case JsNumber(num) => JsSuccess(ScalaDuration(num.toLongExact, TimeUnit.MILLISECONDS))
      case _ => JsError("Invalid num for duration")
    }
  }

}
