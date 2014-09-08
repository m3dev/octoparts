package com.m3.octoparts.model.config.json

import com.m3.octoparts.model.config.ParamType

case class PartParam(required: Boolean,
                     versioned: Boolean,
                     paramType: ParamType.Value,
                     outputName: String,
                     inputNameOverride: Option[String],
                     cacheGroups: Set[CacheGroup])