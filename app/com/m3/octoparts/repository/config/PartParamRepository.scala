package com.m3.octoparts.repository.config

import com.m3.octoparts.model.config._
import scalikejdbc._
import skinny.orm.feature.TimestampsFeature
import skinny.orm.feature.associations.{ BelongsToAssociation, HasManyAssociation }
import skinny.{ ParamType => SkinnyParamType }

import scala.collection.SortedSet

object PartParamRepository
    extends ConfigMapper[PartParam]
    with TimestampsFeature[PartParam] {

  lazy val defaultAlias = createAlias("part_param")

  override val tableName = "part_param"

  /**
   * Overridden to allow us to persist the CacheGroups associated with a PartParam
   */
  override def save(param: PartParam)(implicit s: DBSession = autoSession): Long = {
    param.id.fold {
      val id = createWithPermittedAttributes(permitted(param))
      saveCacheGroups(param.copy(id = Some(id)))
      id
    } { id =>
      updateById(id).withPermittedAttributes(permitted(param))
      saveCacheGroups(param)
      id
    }
  }

  protected val permittedFields = Seq(
    "httpPartConfigId" -> SkinnyParamType.Long,
    "required" -> SkinnyParamType.Boolean,
    "versioned" -> SkinnyParamType.Boolean,
    "paramType" -> SkinnyParamType.String,
    "outputName" -> SkinnyParamType.String,
    "description" -> SkinnyParamType.String,
    "inputNameOverride" -> SkinnyParamType.String,
    "cacheGroupId" -> SkinnyParamType.Long
  )

  lazy val httpPartConfigRef: BelongsToAssociation[PartParam] = belongsToWithFk[HttpPartConfig](
    right = HttpPartConfigRepository,
    fk = "httpPartConfigId",
    merge = (pp, mbHpc) => pp.copy(httpPartConfig = mbHpc)
  )

  /*
    Note that you cannot actually tack on .includes to do nested eager loading because it seems to be
    broken with hasManyThrough (e.g. trying to eager load a HttpPartConfig's PartParams' CacheGroups does not work)
   */
  lazy val cacheGroupsRef: HasManyAssociation[PartParam] = hasManyThroughWithFk[CacheGroup](
    through = PartParamCacheGroupRepository,
    many = CacheGroupRepository,
    throughFk = "partParamId",
    manyFk = "cacheGroupId",
    merge = (param, cacheGroups) => param.copy(cacheGroups = cacheGroups.to[SortedSet])
  ).includes[CacheGroup] { (params, cacheGroups) =>
    params.map { param =>
      param.copy(
        cacheGroups = cacheGroups.filter(_.partParams.exists(_.id == param.id)).to[SortedSet]
      )
    }
  }

  // initializes the default references
  {
    cacheGroupsRef.byDefault
  }

  private def saveCacheGroups(param: PartParam)(implicit dbSession: DBSession): Unit = {
    val a = PartParamCacheGroupRepository.defaultAlias
    // Delete all PartParamCacheGroups that currently belong to this PartParam
    PartParamCacheGroupRepository.deleteBy(sqls.eq(a.partParamId, param.id))
    // Insert the collection into the database
    for (cG <- param.cacheGroups) {
      PartParamCacheGroupRepository.createWithAttributes(
        'cacheGroupId -> cG.id,
        'partParamId -> param.id
      )
    }
  }

  def extract(rs: WrappedResultSet, n: ResultName[PartParam]): PartParam = PartParam(
    id = rs.get(n.id),
    httpPartConfigId = rs.get(n.httpPartConfigId),
    required = rs.get(n.required),
    versioned = rs.get(n.versioned),
    paramType = ParamType.withName(rs.get(n.paramType)),
    outputName = rs.get(n.outputName),
    inputNameOverride = rs.get(n.inputNameOverride),
    description = rs.get(n.description),
    createdAt = rs.get(n.createdAt),
    updatedAt = rs.get(n.updatedAt)
  )

  /**
   * Returns a collection of params that belong to a part
   */
  def findByPartId(partId: Long)(implicit dbSession: DBSession): SortedSet[PartParam] = {
    findAllBy(sqls.eq(defaultAlias.httpPartConfigId, partId)).to[SortedSet]
  }

}
