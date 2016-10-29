package com.m3.octoparts.cache.key

import com.m3.octoparts.cache.versioning.LatestVersionCache.Version
import com.m3.octoparts.model.config.ShortPartParam

case class PartCacheKey(
  partId: String,
  versions: Seq[Version],
  paramValues: Map[ShortPartParam, Seq[String]]
)
    extends CacheKey
