package com.m3.octoparts.repository.config

import com.m3.octoparts.model.config._
import scalikejdbc._
import skinny.orm.feature.TimestampsFeature
import skinny.{ ParamType => SkinnyParamType }

import scala.concurrent.duration._
import scala.language.postfixOps

object HystrixConfigRepository extends ConfigMapper[HystrixConfig] with TimestampsFeature[HystrixConfig] {

  override lazy val defaultAlias = createAlias("hystrix_config")

  override lazy val tableName = "hystrix_config"

  protected val permittedFields = Seq(
    "httpPartConfigId" -> SkinnyParamType.Long,
    "threadPoolConfigId" -> SkinnyParamType.Long,
    "commandKey" -> SkinnyParamType.String,
    "commandGroupKey" -> SkinnyParamType.String,
    "localContentsAsFallback" -> SkinnyParamType.Boolean,
    "timeout" -> ExtraParamType.FineDurationParamType
  )

  // #byDefault is used for now in case we ever need to refer back to the HttpPartConfig
  // to which this HystrixConfig belongs
  lazy val httpPartConfigOpt = belongsToWithFk[HttpPartConfig](
    HttpPartConfigRepository, "httpPartConfigId", (hc, mbHpc) => hc.copy(httpPartConfig = mbHpc)
  )

  /*
    #byDefault should always be used here because we need to be able to eager load the thread
    configuration when fetching HttpPartConfigs
  */
  lazy val threadConfigOpt = {
    belongsToWithFk[ThreadPoolConfig](
      ThreadPoolConfigRepository, "threadPoolConfigId", (h, c) => h.copy(threadPoolConfig = c)
    ).includes[ThreadPoolConfig](merge = (hystrixConfigs, threadConfigs) =>
        hystrixConfigs.map { h =>
          threadConfigs.collectFirst {
            case tpc if h.threadPoolConfigId == tpc.id => h.copy(threadPoolConfig = Some(tpc))
          }.getOrElse(h)
        })
  }

  // initializes the default references
  {
    httpPartConfigOpt.byDefault
    threadConfigOpt.byDefault
  }

  def extract(rs: WrappedResultSet, n: ResultName[HystrixConfig]) = HystrixConfig(
    id = rs.get(n.id),
    httpPartConfigId = rs.get(n.httpPartConfigId),
    threadPoolConfigId = rs.get(n.threadPoolConfigId),
    commandKey = rs.string(n.commandKey),
    commandGroupKey = rs.string(n.commandGroupKey),
    timeout = rs.long(n.timeout).millis,
    localContentsAsFallback = rs.get(n.localContentsAsFallback),
    createdAt = rs.get(n.createdAt),
    updatedAt = rs.get(n.updatedAt)
  )
}
