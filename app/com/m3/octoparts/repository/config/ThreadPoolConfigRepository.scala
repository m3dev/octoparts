package com.m3.octoparts.repository.config

import com.m3.octoparts.model.config._
import scalikejdbc._
import skinny.orm.feature.TimestampsFeature
import skinny.{ ParamType => SkinnyParamType }

import scala.collection.SortedSet

object ThreadPoolConfigRepository extends ConfigMapper[ThreadPoolConfig] with TimestampsFeature[ThreadPoolConfig] {

  lazy val defaultAlias = createAlias("thread_pool_config")

  override val tableName = "thread_pool_config"

  protected val permittedFields = Seq(
    "threadPoolKey" -> SkinnyParamType.String,
    "coreSize" -> SkinnyParamType.Int,
    "queueSize" -> SkinnyParamType.Int
  )

  lazy val hystrixConfigRef = hasMany[HystrixConfig](
    many = HystrixConfigRepository -> HystrixConfigRepository.defaultAlias,
    // defines join condition by using aliases
    on = (t, h) => sqls.eq(t.id, h.threadPoolConfigId),
    // function to merge associations to main entity
    merge = (tpc, hcs) => tpc.copy(hystrixConfigs = hcs.toSet)
  ).includes[HystrixConfig]((tpcs, hcs) => tpcs.map(tpc => tpc.copy(hystrixConfigs = hcs.filter(_.threadPoolConfigId == tpc.id).toSet)))

  def extract(rs: WrappedResultSet, n: ResultName[ThreadPoolConfig]) = ThreadPoolConfig(
    id = rs.get(n.id),
    threadPoolKey = rs.get(n.threadPoolKey),
    coreSize = rs.get(n.coreSize),
    queueSize = rs.get(n.queueSize),
    createdAt = rs.get(n.createdAt),
    updatedAt = rs.get(n.updatedAt)
  )

}