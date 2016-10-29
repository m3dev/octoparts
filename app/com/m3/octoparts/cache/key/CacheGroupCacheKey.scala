package com.m3.octoparts.cache.key

case class CacheGroupCacheKey[T <: java.io.Serializable](
  id: T
) extends CacheKey
