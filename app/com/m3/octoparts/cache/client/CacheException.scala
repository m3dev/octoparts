package com.m3.octoparts.cache.client

import com.m3.octoparts.cache.key.CacheKey

case class CacheException(key: CacheKey, cause: Throwable) extends Exception(cause)
