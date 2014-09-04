package com.m3.octoparts.cache.directive

import com.m3.octoparts.cache.config.CacheConfig
import com.m3.octoparts.cache.versioning.VersionedParamKey
import com.m3.octoparts.model.config.ShortPartParam

trait CacheDirectiveGenerator {

  /**
   * Generate a cache directive, i.e. all the information the cache client needs to lookup/insert a PartResponse.
   */
  def generateDirective(partId: String,
                        partRequestArgs: Map[ShortPartParam, String],
                        cacheConfig: CacheConfig): CacheDirective
}

object CacheDirectiveGenerator extends CacheDirectiveGenerator {
  override def generateDirective(partId: String, partRequestArgs: Map[ShortPartParam, String], cacheConfig: CacheConfig): CacheDirective = {
    val strParams = partRequestArgs.map {
      case (k, v) => k.outputName -> v
    }
    val versionedParamKeys = cacheConfig.versionedParams.map {
      versionedParam =>
        VersionedParamKey(partId, versionedParam, strParams.getOrElse(versionedParam, null))
    }
    CacheDirective(partId, versionedParamKeys, partRequestArgs, cacheConfig.ttl)
  }
}