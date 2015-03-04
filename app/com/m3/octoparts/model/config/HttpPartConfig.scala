package com.m3.octoparts.model.config

import com.m3.octoparts.cache.config.CacheConfig
import com.m3.octoparts.model.HttpMethod
import com.m3.octoparts.model.config.json.{ HttpPartConfig => JsonHttpPartConfig, AlertMailSettings }
import org.joda.time.DateTime

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try

/**
 * Model for holding configuration data for a Http dependency that
 * comes with a companion-object that can populate it from the database
 *
 * @param localContentsEnabled whether local contents is enabled
 * @param localContents the static contents which is used instead of actual contents of this part
 */
case class HttpPartConfig(id: Option[Long] = None, // None means that the record is new
                          partId: String,
                          owner: String,
                          description: Option[String],
                          uriToInterpolate: String,
                          method: HttpMethod.Value,
                          additionalValidStatuses: Set[Int] = Set.empty,
                          httpPoolSize: Int,
                          httpConnectionTimeout: FiniteDuration,
                          httpSocketTimeout: FiniteDuration,
                          httpDefaultEncoding: Charset,
                          httpProxy: Option[String] = None,
                          parameters: Set[PartParam] = Set.empty,
                          hystrixConfig: Option[HystrixConfig] = None,
                          deprecatedInFavourOf: Option[String] = None,
                          cacheGroups: Set[CacheGroup] = Set.empty,
                          cacheTtl: Option[FiniteDuration] = Some(Duration.Zero), // in seconds
                          alertMailsEnabled: Boolean,
                          alertAbsoluteThreshold: Option[Int],
                          alertPercentThreshold: Option[Double],
                          alertInterval: FiniteDuration,
                          alertMailRecipients: Option[String],
                          localContentsEnabled: Boolean,
                          localContents: Option[String],
                          createdAt: DateTime,
                          updatedAt: DateTime) extends ConfigModel[HttpPartConfig] {
  /**
   * Method to use when we are sure we have a HystrixConfig inside the
   * hystrixConfig field.
   * @return HystrixConfig
   */
  def hystrixConfigItem: HystrixConfig = hystrixConfig.get

  def cacheConfig: CacheConfig = {
    // make sure the order of parameters never changes (using DB id)
    val versionedParamNames = parameters.filter(_.versioned).toSeq.sortBy(_.id).map(_.outputName)
    CacheConfig(cacheTtl, versionedParamNames)
  }

  def httpProxySettings: Option[HttpProxySettings] = httpProxy.flatMap(HttpProxySettings.parse(_).toOption)

}

object HttpPartConfig {

  def parseValidStatuses(mbVal: Option[String]): Set[Int] = mbVal.fold(Set.empty[Int]) {
    _.split(",").flatMap { tok =>
      Try {
        tok.trim.toInt
      }.toOption
    }.toSet
  }

  /**
   * Returns a [[JsonHttpPartConfig]] for a [[HttpPartConfig]]
   */
  def toJsonModel(config: HttpPartConfig): JsonHttpPartConfig = {
    require(config.hystrixConfig.isDefined, s"HystrixConfig not defined on $config")
    JsonHttpPartConfig(
      partId = config.partId,
      owner = config.owner,
      description = config.description,
      uriToInterpolate = config.uriToInterpolate,
      method = config.method,
      hystrixConfig = HystrixConfig.toJsonModel(config.hystrixConfig.get),
      additionalValidStatuses = config.additionalValidStatuses,
      httpPoolSize = config.httpPoolSize,
      httpConnectionTimeout = config.httpConnectionTimeout,
      httpSocketTimeout = config.httpSocketTimeout,
      httpDefaultEncoding = config.httpDefaultEncoding.underlying,
      httpProxy = config.httpProxy,
      parameters = config.parameters.map(PartParam.toJsonModel),
      deprecatedInFavourOf = config.deprecatedInFavourOf,
      cacheGroups = config.cacheGroups.map(CacheGroup.toJsonModel),
      cacheTtl = config.cacheTtl,
      alertMailSettings = AlertMailSettings(alertMailsEnabled = config.alertMailsEnabled,
        alertAbsoluteThreshold = config.alertAbsoluteThreshold,
        alertPercentThreshold = config.alertPercentThreshold,
        alertInterval = config.alertInterval,
        alertMailRecipients = config.alertMailRecipients),
      localContentsEnabled = config.localContentsEnabled,
      localContents = config.localContents
    )
  }

  def fromJsonModel(config: JsonHttpPartConfig): HttpPartConfig = {
    HttpPartConfig(
      partId = config.partId,
      owner = config.owner,
      description = config.description,
      uriToInterpolate = config.uriToInterpolate,
      method = config.method,
      hystrixConfig = Some(HystrixConfig.fromJsonModel(config.hystrixConfig)),
      additionalValidStatuses = config.additionalValidStatuses,
      httpPoolSize = config.httpPoolSize,
      httpConnectionTimeout = config.httpConnectionTimeout,
      httpSocketTimeout = config.httpSocketTimeout,
      httpDefaultEncoding = Charset.forName(config.httpDefaultEncoding.name),
      httpProxy = config.httpProxy,
      parameters = config.parameters.map(PartParam.fromJsonModel),
      deprecatedInFavourOf = config.deprecatedInFavourOf,
      cacheGroups = config.cacheGroups.map(CacheGroup.fromJsonModel),
      cacheTtl = config.cacheTtl,
      alertMailsEnabled = config.alertMailSettings.alertMailsEnabled,
      alertAbsoluteThreshold = config.alertMailSettings.alertAbsoluteThreshold,
      alertPercentThreshold = config.alertMailSettings.alertPercentThreshold,
      alertInterval = config.alertMailSettings.alertInterval,
      alertMailRecipients = config.alertMailSettings.alertMailRecipients,
      localContentsEnabled = config.localContentsEnabled,
      localContents = config.localContents,
      createdAt = DateTime.now,
      updatedAt = DateTime.now
    )
  }

}
