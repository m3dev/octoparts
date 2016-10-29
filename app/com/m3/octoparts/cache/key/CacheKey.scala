package com.m3.octoparts.cache.key

/**
 * A key that can be used to lookup a value in a cache.
 * this is all we ask for a CacheKey. Implementations are client-specific.
 */
trait CacheKey extends Serializable
