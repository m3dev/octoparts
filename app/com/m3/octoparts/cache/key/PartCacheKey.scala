package com.m3.octoparts.cache.key

import com.m3.octoparts.cache.versioning.LatestVersionCache._
import com.m3.octoparts.model.config.ShortPartParamValue

case class PartCacheKey(
  partId: String,
  versions: Seq[Version],
  paramValues: Set[ShortPartParamValue])
    extends CacheKey
