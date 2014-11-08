package com.m3.octoparts.json.format

import java.util.concurrent.TimeUnit

import play.api.libs.json._

import scala.concurrent.duration.FiniteDuration

object Duration {

  implicit val format = new Format[FiniteDuration] {
    def writes(o: FiniteDuration): JsValue = JsNumber(o.toMillis)
    def reads(json: JsValue): JsResult[FiniteDuration] = json match {
      case JsNumber(num) => JsSuccess(FiniteDuration(num.toLongExact, TimeUnit.MILLISECONDS))
      case _ => JsError("Invalid num for duration")
    }
  }

}
