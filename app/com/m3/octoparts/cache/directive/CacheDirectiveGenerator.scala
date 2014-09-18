package com.m3.octoparts.cache.directive

import com.m3.octoparts.cache.config.CacheConfig
import com.m3.octoparts.cache.versioning.VersionedParamKey
import com.m3.octoparts.model.config.ShortPartParamValue

trait CacheDirectiveGenerator {

  /**
   * Generate a cache directive, i.e. all the information the cache client needs to lookup/insert a PartResponse.
   */
  def generateDirective(partId: String,
                        paramValues: Set[ShortPartParamValue],
                        cacheConfig: CacheConfig): CacheDirective
}

object CacheDirectiveGenerator extends CacheDirectiveGenerator {
  override def generateDirective(partId: String, paramValues: Set[ShortPartParamValue], cacheConfig: CacheConfig): CacheDirective = {
    // for version param keys, only single-value parameters are supported.
    val strParams = paramValues.map {
      sppv =>
        sppv.shortPartParam.outputName -> sppv.values
    }.toMap
    val versionedParamKeys = cacheConfig.versionedParams.map {
      versionedParam =>
        // FIXME document why null is acceptable here
        val vp = strParams.getOrElse(versionedParam, Nil).headOption.orNull
        VersionedParamKey(partId, versionedParam, vp)
    }
    CacheDirective(partId, versionedParamKeys, paramValues, cacheConfig.ttl)
  }
}