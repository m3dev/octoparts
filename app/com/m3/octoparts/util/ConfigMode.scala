package com.m3.octoparts.util

final case class ConfigMode(value: String) extends AnyVal {

  def isProd: Boolean = value.toLowerCase.startsWith("prod")

}
