package com.m3.octoparts.cache.config

import scala.concurrent.duration.Duration

/**
 * All configuration related to caching for a given part.
 * This config can be combined with information about the parameters of an individual part request
 * in order to create a cache directive for the part request.
 */
case class CacheConfig(
    ttl: Option[Duration] = None,
    versionedParams: Seq[String] = Nil) {
  // caching is enabled if the TTL is not <= 0
  def cachingEnabled: Boolean = ttl.fold(true)(_.toSeconds > 0)
}

object CacheConfig {
  val NoCache = CacheConfig()
}

