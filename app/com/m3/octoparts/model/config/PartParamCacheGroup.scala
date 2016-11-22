package com.m3.octoparts.model.config

// Intersection class and mapper to facilitate many-to-many with CacheGroup
case class PartParamCacheGroup(
  cacheGroupId: Long,
  partParamId: Long
)
