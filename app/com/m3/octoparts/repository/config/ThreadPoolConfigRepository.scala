package com.m3.octoparts.repository.config

import com.m3.octoparts.model.config._
import scalikejdbc._
import skinny.orm.feature.TimestampsFeature
import skinny.{ ParamType => SkinnyParamType }

object ThreadPoolConfigRepository extends ConfigMapper[ThreadPoolConfig] with TimestampsFeature[ThreadPoolConfig] {

  override lazy val defaultAlias = createAlias("thread_pool_config")

  override lazy val tableName = "thread_pool_config"

  override val permittedFields = Seq(
    "threadPoolKey" -> SkinnyParamType.String,
    "coreSize" -> SkinnyParamType.Int
  )

  lazy val hystrixCommandRef = hasMany[HystrixConfig](
    many = HystrixConfigRepository -> HystrixConfigRepository.defaultAlias,
    // defines join condition by using aliases
    on = (t, h) => sqls.eq(t.id, h.threadPoolConfigId),
    // function to merge associations to main entity
    merge = (t, h) => t.copy(hystrixConfigs = h)
  )

  def extract(rs: WrappedResultSet, n: ResultName[ThreadPoolConfig]) = ThreadPoolConfig(
    id = rs.get(n.id),
    threadPoolKey = rs.get(n.threadPoolKey),
    coreSize = rs.get(n.coreSize),
    createdAt = rs.get(n.createdAt),
    updatedAt = rs.get(n.updatedAt)
  )

}