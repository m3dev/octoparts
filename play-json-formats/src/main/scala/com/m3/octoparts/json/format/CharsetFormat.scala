package com.m3.octoparts.json.format

import java.nio.charset.Charset

import play.api.libs.json._

import scala.util.{ Failure, Success, Try }

object CharsetFormat {

  implicit val charsetFormat = new Format[Charset] {
    def writes(o: Charset): JsValue = JsString(o.name())

    def reads(json: JsValue): JsResult[Charset] = json match {
      case JsString(name) => Try {
        Charset.forName(name)
      } match {
        case Success(charset) => JsSuccess(charset)
        case Failure(err) => JsError(err.toString)
      }
      case _ => JsError("Invalid charset")
    }
  }

}
