package com.m3.octoparts.cache.key

case class VersionCacheKey[T <: java.io.Serializable](id: T) extends CacheKey
