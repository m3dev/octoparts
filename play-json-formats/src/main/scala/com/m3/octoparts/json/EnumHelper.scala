package com.m3.octoparts.json

import play.api.libs.json._

/**
 * Holds helper methods for generating JSON [[play.api.libs.json.Writes]], [[play.api.libs.json.Reads]]
 * and [[play.api.libs.json.Format]] for [[Enumeration]] objects
 */
object EnumerationHelper {

  /**
   * Generates a [[Format]] for the given [[Enumeration]]
   */
  def formats[E <: Enumeration](enum: E): Format[E#Value] = {
    Format(reads(enum), writes)
  }

  /**
   * Generates a [[Reads]] for the given [[Enumeration]]
   */
  def reads[E <: Enumeration](enum: E): Reads[E#Value] = new Reads[E#Value] {
    def reads(json: JsValue): JsResult[E#Value] = json match {
      case JsString(s) => {
        try {
          JsSuccess(enum.withName(s))
        } catch {
          case _: NoSuchElementException => JsError(s"Enumeration expected of type: '${enum.getClass}', but it does not appear to contain the value: '$s'")
        }
      }
      case _ => JsError("String value expected")
    }
  }

  /**
   * Generates a [[Writes]] for the [[Enumeration]] type
   */
  def writes[E <: Enumeration]: Writes[E#Value] = new Writes[E#Value] {
    def writes(v: E#Value): JsValue = JsString(v.toString)
  }

}
