package com.m3.octoparts.cache.key

import com.m3.octoparts.cache.versioning.LatestVersionCache._
import com.m3.octoparts.model.config.ShortPartParam

case class PartCacheKey(
  partId: String,
  versions: Seq[Version],
  paramMap: Map[ShortPartParam, String])
    extends CacheKey
