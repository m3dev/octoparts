package com.m3.octoparts.model.config

case class ShortPartParam(
  outputName: String,
  paramType: ParamType.Value
)

object ShortPartParam {

  def apply(partParam: PartParam): ShortPartParam = apply(partParam.outputName, partParam.paramType)

}
