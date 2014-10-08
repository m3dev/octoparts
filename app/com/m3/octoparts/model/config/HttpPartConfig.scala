package com.m3.octoparts.model.config

import com.m3.octoparts.cache.config.CacheConfig
import com.m3.octoparts.model.HttpMethod
import com.m3.octoparts.model.config.json.{ HttpPartConfig => JsonHttpPartConfig }
import org.joda.time.DateTime

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try

/**
 * Model for holding configuration data for a Http dependency that
 * comes with a companion-object that can populate it from the database
 */
case class HttpPartConfig(id: Option[Long] = None, // None means that the record is new
                          partId: String,
                          owner: String,
                          uriToInterpolate: String,
                          description: Option[String],
                          method: HttpMethod.Value,
                          additionalValidStatuses: Set[Int] = Set.empty,
                          parameters: Set[PartParam] = Set.empty,
                          hystrixConfig: Option[HystrixConfig] = None,
                          deprecatedInFavourOf: Option[String] = None,
                          cacheGroups: Set[CacheGroup] = Set.empty,
                          cacheTtl: Option[Duration] = Some(Duration.Zero), // in seconds
                          alertMailsEnabled: Boolean,
                          alertAbsoluteThreshold: Option[Int],
                          alertPercentThreshold: Option[Double],
                          alertInterval: Duration,
                          alertMailRecipients: Option[String],
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
      uriToInterpolate = config.uriToInterpolate,
      description = config.description,
      method = config.method,
      hystrixConfig = HystrixConfig.toJsonModel(config.hystrixConfig.get),
      additionalValidStatuses = config.additionalValidStatuses,
      parameters = config.parameters.map(PartParam.toJsonModel),
      deprecatedInFavourOf = config.deprecatedInFavourOf,
      cacheGroups = config.cacheGroups.map(CacheGroup.toJsonModel),
      cacheTtl = config.cacheTtl,
      alertMailsEnabled = config.alertMailsEnabled,
      alertAbsoluteThreshold = config.alertAbsoluteThreshold,
      alertPercentThreshold = config.alertPercentThreshold,
      alertInterval = config.alertInterval,
      alertMailRecipients = config.alertMailRecipients
    )
  }

  def fromJsonModel(config: JsonHttpPartConfig): HttpPartConfig = {
    HttpPartConfig(
      partId = config.partId,
      owner = config.owner,
      uriToInterpolate = config.uriToInterpolate,
      description = config.description,
      method = config.method,
      hystrixConfig = Some(HystrixConfig.fromJsonModel(config.hystrixConfig)),
      additionalValidStatuses = config.additionalValidStatuses,
      parameters = config.parameters.map(PartParam.fromJsonModel),
      deprecatedInFavourOf = config.deprecatedInFavourOf,
      cacheGroups = config.cacheGroups.map(CacheGroup.fromJsonModel),
      cacheTtl = config.cacheTtl,
      alertMailsEnabled = config.alertMailsEnabled,
      alertAbsoluteThreshold = config.alertAbsoluteThreshold,
      alertPercentThreshold = config.alertPercentThreshold,
      alertInterval = config.alertInterval,
      alertMailRecipients = config.alertMailRecipients,
      createdAt = DateTime.now,
      updatedAt = DateTime.now
    )
  }

}
