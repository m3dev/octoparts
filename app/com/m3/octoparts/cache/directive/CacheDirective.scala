package com.m3.octoparts.cache.directive

import com.m3.octoparts.cache.versioning.VersionedParamKey
import com.m3.octoparts.model.config.ShortPartParamValue

import scala.concurrent.duration.Duration

/**
 * Information that the cache client needs when getting a value from or inserting a value into the cache
 *
 * @param versionedParamKeys a list of parameters that are versioned
 * @param ttl Time To Live
 */
case class CacheDirective(partId: String,
                          versionedParamKeys: Seq[VersionedParamKey] = Nil,
                          paramValues: Set[ShortPartParamValue] = Set.empty,
                          ttl: Option[Duration] = None)
