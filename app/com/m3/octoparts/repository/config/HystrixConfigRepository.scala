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
    "timeoutInMs" -> SkinnyParamType.Long
  )

  // #byDefault is used for now in case we ever need to refer back to the HttpPartConfig
  // to which this HystrixConfig belongs
  lazy val httpPartConfigOpt = belongsToWithFk[HttpPartConfig](
    HttpPartConfigRepository, "httpPartConfigId", (h, c) => h.copy(httpPartConfig = c))

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
            case t if h.threadPoolConfigId == t.id => h.copy(threadPoolConfig = Some(t))
          }.getOrElse(h)
        }
      )
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
    commandKey = rs.get(n.commandKey),
    commandGroupKey = rs.get(n.commandGroupKey),
    timeoutInMs = rs.get(n.timeoutInMs),
    localContentsAsFallback = rs.get(n.localContentsAsFallback),
    createdAt = rs.get(n.createdAt),
    updatedAt = rs.get(n.updatedAt))

}
