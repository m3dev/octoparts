package com.m3.octoparts.auth

import play.api.libs.json.Json

/** A principal, i.e. an authenticated user */
case class Principal(id: String, nickname: String, email: String, roles: Seq[String])

object Principal {

  implicit val jsonFormat = Json.format[Principal]

}
