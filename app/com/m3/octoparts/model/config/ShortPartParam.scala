package com.m3.octoparts.model.config

object ShortPartParam {
  def apply(partParam: PartParam): ShortPartParam = apply(partParam.outputName, partParam.paramType)
}

case class ShortPartParam(outputName: String, paramType: ParamType.Value)
