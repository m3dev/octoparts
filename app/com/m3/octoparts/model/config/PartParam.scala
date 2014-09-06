package com.m3.octoparts.model.config

import org.joda.time.DateTime
import com.m3.octoparts.model.config.json.{ PartParam => JsonPartParam }

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

  /**
   * This is the key used to look for a value inside the PartRequest
   */
  def inputName: String = inputNameOverride.getOrElse(outputName)

  def shorter = ShortPartParam(outputName, paramType)

}

object PartParam {

  /**
   * Returns a [[JsonPartParam]] for a [[PartParam]]
   */
  def toJsonModel(param: PartParam): JsonPartParam = {
    require(param.id.isDefined && param.httpPartConfigId.isDefined)
    JsonPartParam(
      httpPartConfigId = param.httpPartConfigId.get,
      required = param.required,
      versioned = param.versioned,
      paramType = param.paramType,
      outputName = param.outputName,
      inputNameOverride = param.inputNameOverride,
      cacheGroups = param.cacheGroups.map(CacheGroup.toJsonModel)
    )
  }
}