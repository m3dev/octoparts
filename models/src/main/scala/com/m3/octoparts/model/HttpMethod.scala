package com.m3.octoparts.model

object HttpMethod extends Enumeration {
  type HttpMethod = Value

  val Get = Value("get")
  val Post = Value("post")
  val Put = Value("put")
  val Delete = Value("delete")
  val Head = Value("head")
  val Patch = Value("patch")
  val Options = Value("options")
}
