package com.m3.octoparts.model.config

object ParamType extends Enumeration {
  type ParamType = Value

  val Query = Value("query")
  val Path = Value("path")
  val Header = Value("header")
  val Cookie = Value("cookie")
  val Body = Value("body")
}
