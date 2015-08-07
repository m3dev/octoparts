package com.m3.octoparts.model.config

import org.joda.time.DateTime
import com.m3.octoparts.model.config.json.{ CacheGroup => JsonCacheGroup }

import scala.collection.SortedSet

/**
 * Defines a group of objects that need to have their cache invalidated as a group
 */
case class CacheGroup(id: Option[Long] = None, // None means that the record is new
                      name: String,
                      owner: String,
                      description: String = "",
                      httpPartConfigs: SortedSet[HttpPartConfig] = SortedSet.empty,
                      partParams: SortedSet[PartParam] = SortedSet.empty,
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

  def fromJsonModel(cacheGroup: JsonCacheGroup): CacheGroup = {
    CacheGroup(
      name = cacheGroup.name,
      owner = cacheGroup.owner,
      description = cacheGroup.description,
      createdAt = DateTime.now,
      updatedAt = DateTime.now
    )
  }

  implicit val order: Ordering[CacheGroup] = Ordering.by(_.name)
}