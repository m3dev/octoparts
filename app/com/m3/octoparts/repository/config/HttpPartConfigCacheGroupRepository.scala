package com.m3.octoparts.repository.config

import com.m3.octoparts.model.config.HttpPartConfigCacheGroup
import skinny.orm.SkinnyJoinTable

object HttpPartConfigCacheGroupRepository extends SkinnyJoinTable[HttpPartConfigCacheGroup] {

  lazy val defaultAlias = createAlias("http_part_config_cache_group")

  override val tableName = "http_part_config_cache_group"

}
