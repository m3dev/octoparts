package com.m3.octoparts.model.config.json

import com.m3.octoparts.model.config.ParamType

/**
 * Created by Lloyd on 9/6/14.
 */
case class PartParam(httpPartConfigId: Long,
                     required: Boolean,
                     versioned: Boolean,
                     paramType: ParamType.Value,
                     outputName: String,
                     inputNameOverride: Option[String],
                     cacheGroups: Set[CacheGroup])