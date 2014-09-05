package com.m3.octoparts.model.config

import com.m3.octoparts.cache.config.CacheConfig
import com.m3.octoparts.http.HttpMethod
import com.m3.octoparts.repository.config.HttpPartConfigRepository
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
                          description: String,
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

}
