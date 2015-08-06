package com.m3.octoparts.cache.directive

import com.m3.octoparts.cache.config.CacheConfig
import com.m3.octoparts.cache.versioning.VersionedParamKey
import com.m3.octoparts.model.config.ShortPartParam

trait CacheDirectiveGenerator {

  /**
   * Generate a cache directive, i.e. all the information the cache client needs to lookup/insert a PartResponse.
   */
  def generateDirective(
    partId:      String,
    params:      Map[ShortPartParam, Seq[String]],
    cacheConfig: CacheConfig
  ): CacheDirective
}

object CacheDirectiveGenerator extends CacheDirectiveGenerator {
  def generateDirective(partId: String, params: Map[ShortPartParam, Seq[String]], cacheConfig: CacheConfig): CacheDirective = {
    // for version param keys, only single-value parameters are supported.
    val strParams = params.map {
      case (shortPartParam, values) =>
        shortPartParam.outputName -> values
    }
    val versionedParamKeys = cacheConfig.versionedParams.map {
      versionedParam =>
        // FIXME document why null is acceptable here
        val vp = strParams.getOrElse(versionedParam, Nil).headOption.orNull
        VersionedParamKey(partId, versionedParam, vp)
    }
    CacheDirective(partId, versionedParamKeys, params, cacheConfig.ttl)
  }
}