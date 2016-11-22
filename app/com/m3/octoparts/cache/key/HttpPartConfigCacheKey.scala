package com.m3.octoparts.cache.key

/*
 * We should use different case classes for each kind of object we want to through
 * into the cache because it allows us to ensure that each key generated is unique
 */
case class HttpPartConfigCacheKey[T <: java.io.Serializable](
  id: T
) extends CacheKey
