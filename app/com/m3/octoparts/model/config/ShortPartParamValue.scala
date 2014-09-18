package com.m3.octoparts.model.config

/**
 * @param values should not be empty
 */
case class ShortPartParamValue(shortPartParam: ShortPartParam, values: Seq[String]) {
  def this(shortPartParam: ShortPartParam, value: String) = this(shortPartParam, Seq(value))
  def this(outputName: String, paramType: ParamType.Value, value: String) = this(ShortPartParam(outputName, paramType), value)
}
