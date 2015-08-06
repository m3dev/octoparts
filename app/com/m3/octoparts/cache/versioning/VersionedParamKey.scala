package com.m3.octoparts.cache.versioning

/**
 * The name and value of a versioned parameter.
 * This information is everything we need to use as a key for a cache version.
 * e.g. VersionedParamKey("userInfoPart", "userId", "123") may have a cache version of 99.
 */
case class VersionedParamKey(
  partId:    String,
  paramName: String,
  value:     String
)

