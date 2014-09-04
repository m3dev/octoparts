package com.m3.octoparts.repository.config

import com.m3.octoparts.model.config.PartParamCacheGroup
import skinny.orm.SkinnyJoinTable

object PartParamCacheGroupRepository extends SkinnyJoinTable[PartParamCacheGroup] {

  override lazy val defaultAlias = createAlias("part_param_cache_group")

  override lazy val tableName = "part_param_cache_group"

}