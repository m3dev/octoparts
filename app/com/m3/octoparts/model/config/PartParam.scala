package com.m3.octoparts.model.config

import com.m3.octoparts.repository.config.PartParamRepository
import org.joda.time.DateTime
import skinny.{ ParamType => SkinnyParamType }

/**
 * Model for holding Parameter configuration data for a Http dependency that
 * comes with a companion-object that can populate it from the database
 *
 * @param required if this Parameter is required when invoking this param's dependency
 * @param paramType Parameter Type
 * @param outputName the "rendered" name of this parameter when it gets passed during the invocation of the
 *                   dependency to which it belongs.
 * @param inputNameOverride an override for how you wish to pass this parameter. Useful if you want to have
 *                          meaningful names (e.g. 'dogId' can be used instead of 'd')
 */
case class PartParam(
    id: Option[Long] = None, //Implies not yet inserted into the DB
    httpPartConfigId: Option[Long] = None,
    httpPartConfig: Option[HttpPartConfig] = None,
    required: Boolean,
    versioned: Boolean,
    paramType: ParamType.Value,
    outputName: String,
    inputNameOverride: Option[String] = None,
    cacheGroups: Set[CacheGroup] = Set.empty,
    createdAt: DateTime,
    updatedAt: DateTime) extends ConfigModel[PartParam] {

  override def mapper = PartParamRepository

  /**
   * This is the key used to look for a value inside the PartRequest
   */
  def inputName: String = inputNameOverride.getOrElse(outputName)

  def shorter = ShortPartParam(outputName, paramType)

}
