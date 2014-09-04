package com.m3.octoparts.repository.config

import com.m3.octoparts.http.HttpMethod
import com.m3.octoparts.model.config._
import scalikejdbc._
import skinny.orm._
import skinny.orm.feature.TimestampsFeature
import skinny.util.LTSV
import skinny.{ AbstractParamType, ParamType => SkinnyParamType }

import scala.concurrent.duration._
import scala.language.postfixOps

object HttpPartConfigRepository extends ConfigMapper[HttpPartConfig] with TimestampsFeature[HttpPartConfig] {

  override lazy val defaultAlias = createAlias("http_part_config")

  override lazy val tableName = "http_part_config"

  protected val permittedFields = Seq(
    "partId" -> SkinnyParamType.String,
    "owner" -> SkinnyParamType.String,
    "uriToInterpolate" -> SkinnyParamType.String,
    "description" -> SkinnyParamType.String,
    "method" -> SkinnyParamType.String,
    "additionalValidStatuses" -> IntSetParamType,
    "deprecatedInFavourOf" -> SkinnyParamType.String,
    "cacheGroupId" -> SkinnyParamType.Long,
    "cacheTtl" -> DurationParamType,
    "alertMailsEnabled" -> SkinnyParamType.Boolean,
    "alertAbsoluteThreshold" -> SkinnyParamType.Int,
    "alertPercentThreshold" -> SkinnyParamType.Double,
    "alertInterval" -> DurationParamType,
    "alertMailRecipients" -> SkinnyParamType.String
  )

  case object DurationParamType extends AbstractParamType({
    case d: Duration => d.toSeconds
  })

  case object IntSetParamType extends AbstractParamType({
    // this will actually be a Set[Int] but the compiler complains about it.
    case s: Set[_] => s.mkString(",")
  })

  /**
   * Overridden version of .save to allow us to save nested children objects
   *
   * Does not deal with deletion of child objects. If deleting, the call site should
   * make sure to deal with this properly with regards to caching.
   *
   * @param c The HttpPartConfig we want to save, with possible children models
   * @param s DBSession for saving
   * @return Long, the id of the model that was saved
   */
  override def save(c: HttpPartConfig)(implicit s: DBSession = autoSession): Long = c.id.fold {
    val newId = createWithPermittedAttributes(permitted(c))
    c.parameters.foreach(p => PartParamRepository.save(p.copy(httpPartConfigId = Some(newId.toLong))))
    c.hystrixConfig.foreach(h => HystrixConfigRepository.save(h.copy(httpPartConfigId = Some(newId.toLong))))
    saveCacheGroups(c.copy(id = Some(newId)))
    info(LTSV.dump(
      "Part" -> c.partId,
      "Action" -> "Updated",
      "Part data" -> c.toString
    ))
    newId
  } {
    id =>
      updateById(id).withPermittedAttributes(permitted(c))
      c.parameters.foreach(PartParamRepository.save)
      c.hystrixConfig.foreach(HystrixConfigRepository.save)
      saveCacheGroups(c)
      info(LTSV.dump(
        "Part" -> c.partId,
        "Action" -> "Inserted",
        "Part data" -> c.toString
      ))
      id
  }

  /*
    byDefault used because we generally want to have the parameters for a given PartConfig

    _NOTE:_ There is no point in turning this or cacheGroupsRef into .includes associations and
    trying to use them with eager-loading because as of writing, the hasMany->hasManyThrough
    eager loading seems bugged
  */
  lazy val paramsRef = hasMany[PartParam](
    many = PartParamRepository -> PartParamRepository.defaultAlias,
    // defines join condition by using aliases
    on = (c, p) => sqls.eq(c.id, p.httpPartConfigId),
    // function to merge associations to main entity
    merge = (c, params) => c.copy(parameters = params.toSet)
  )

  /*
    References the collection of CacheGroups that each HttpPartConfig has; note that you cannot
    add .byDefault on this reference because CacheGroup also defines the same hasManyThrough
    relationship and .byDefault relationships pointing at each other does NOT work.

    See https://github.com/skinny-framework/skinny-framework/commit/13a87c7a3f671c3c77535426ead117cf15e431a9
   */
  lazy val cacheGroupsRef = hasManyThrough[HttpPartConfigCacheGroup, CacheGroup](
    through = HttpPartConfigCacheGroupRepository -> HttpPartConfigCacheGroupRepository.createAlias("httpPartTestGroupJoin"),
    throughOn = (m1: Alias[HttpPartConfig], m2: Alias[HttpPartConfigCacheGroup]) => sqls.eq(m1.id, m2.httpPartConfigId),
    many = CacheGroupRepository -> CacheGroupRepository.createAlias("cacheGroupJoin"),
    on = (m1: Alias[HttpPartConfigCacheGroup], m2: Alias[CacheGroup]) => sqls.eq(m1.cacheGroupId, m2.id),
    merge = (part, cacheGroups) => part.copy(cacheGroups = cacheGroups.toSet)
  )

  /*
   This is the magic hasOne+includes definition that allows us to fetch a HttpPartConfig's HystrixConfig AND its
   ThreadPoolConfig at the same time without N+1 queries
    */
  lazy val hystrixConfigRef = hasOneWithFk[HystrixConfig](
    right = HystrixConfigRepository,
    fk = "httpPartConfigId",
    merge = (c, hc) => c.copy(hystrixConfig = hc)
  ).includes[HystrixConfig](
      merge = (cs, hs) => cs.map { c => c.copy(hystrixConfig = hs.find(_.httpPartConfigId == c.id)) }
    )

  // initializes the default references
  {
    paramsRef.byDefault
    cacheGroupsRef.byDefault
    hystrixConfigRef.byDefault
  }

  private def saveCacheGroups(c: HttpPartConfig)(implicit dbSession: DBSession): Unit = {
    val cg = HttpPartConfigCacheGroupRepository.defaultAlias
    // Delete all HttpPartConfigCacheGroup that currently belong to this HttPartConfig
    HttpPartConfigCacheGroupRepository.deleteBy(sqls.eq(cg.httpPartConfigId, c.id))
    // Insert the collection into the database
    for (cG <- c.cacheGroups) {
      HttpPartConfigCacheGroupRepository.createWithAttributes(
        'cacheGroupId -> cG.id,
        'httpPartConfigId -> c.id
      )
    }
  }

  def extract(rs: WrappedResultSet, n: ResultName[HttpPartConfig]) = HttpPartConfig(
    id = rs.get(n.id),
    partId = rs.get(n.partId),
    owner = rs.get(n.owner),
    uriToInterpolate = rs.get(n.uriToInterpolate),
    description = rs.get(n.description),
    method = HttpMethod.withName(rs.string(n.method)),
    additionalValidStatuses = HttpPartConfig.parseValidStatuses(rs.get(n.additionalValidStatuses)),
    deprecatedInFavourOf = rs.get(n.deprecatedInFavourOf),
    cacheTtl = rs.longOpt(n.cacheTtl).map(_.seconds),
    alertMailsEnabled = rs.get(n.alertMailsEnabled),
    alertAbsoluteThreshold = rs.get(n.alertAbsoluteThreshold),
    alertPercentThreshold = rs.get(n.alertPercentThreshold),
    alertInterval = rs.long(n.alertInterval).seconds,
    alertMailRecipients = rs.get(n.alertMailRecipients),
    createdAt = rs.get(n.createdAt),
    updatedAt = rs.get(n.updatedAt)
  )

}
