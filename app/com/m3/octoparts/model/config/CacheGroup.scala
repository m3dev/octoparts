package com.m3.octoparts.model.config

import org.joda.time.DateTime
import com.m3.octoparts.model.config.json.{ CacheGroup => JsonCacheGroup }
/**
 * Defines a group of objects that need to have their cache invalidated as a group
 */
case class CacheGroup(
  id: Option[Long] = None, // None means that the record is new
  name: String,
  owner: String,
  description: String = "",
  httpPartConfigs: Seq[HttpPartConfig] = Seq.empty,
  partParams: Seq[PartParam] = Seq.empty,
  createdAt: DateTime,
  updatedAt: DateTime) extends ConfigModel[CacheGroup]

object CacheGroup {

  def toJsonModel(cacheGroup: CacheGroup): JsonCacheGroup = {
    JsonCacheGroup(
      name = cacheGroup.name,
      owner = cacheGroup.owner,
      description = cacheGroup.description
    )
  }

}