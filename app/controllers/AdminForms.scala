package controllers

import com.m3.octoparts.model.HttpMethod
import com.m3.octoparts.model.config._
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.data._

import scala.concurrent.duration._

object AdminForms {

  case class PartData(
      partId: String,
      description: Option[String],
      deprecatedTo: Option[String],
      uri: String,
      method: String,
      additionalValidStatuses: Option[String],
      commandKey: String,
      commandGroupKey: String,
      timeoutInMs: Long,
      threadPoolConfigId: Long,
      ttl: Option[Long],
      cacheGroupNames: Seq[String],
      alertMailsEnabled: Boolean,
      alertInterval: Option[Long],
      alertAbsoluteThreshold: Option[Int],
      alertPercentThreshold: Option[BigDecimal],
      alertMailRecipients: Option[String]) {
    data =>

    /** Create a brand new HttpPartConfig using the data input into the form */
    def toNewHttpPartConfig(owner: String, cacheGroups: Set[CacheGroup]): HttpPartConfig = new HttpPartConfig(
      partId = data.partId,
      owner = owner,
      uriToInterpolate = data.uri,
      description = data.description,
      method = HttpMethod.withName(data.method),
      additionalValidStatuses = HttpPartConfig.parseValidStatuses(data.additionalValidStatuses.filterNot(_.isEmpty)),
      hystrixConfig = Some(new HystrixConfig(
        commandKey = data.commandKey,
        commandGroupKey = data.commandGroupKey,
        threadPoolConfigId = Some(data.threadPoolConfigId),
        timeoutInMs = data.timeoutInMs,
        createdAt = DateTime.now,
        updatedAt = DateTime.now
      )),
      deprecatedInFavourOf = data.deprecatedTo,
      cacheTtl = data.ttl.map(_.seconds),
      cacheGroups = cacheGroups,
      alertMailsEnabled = data.alertMailsEnabled,
      alertInterval = data.alertInterval.getOrElse(60L).seconds,
      alertAbsoluteThreshold = data.alertAbsoluteThreshold,
      alertPercentThreshold = data.alertPercentThreshold.map(_.toDouble),
      alertMailRecipients = data.alertMailRecipients,
      createdAt = DateTime.now,
      updatedAt = DateTime.now
    )

    /** Update an existing HttpPartConfig using the data input into the form */
    def toUpdatedHttpPartConfig(originalPart: HttpPartConfig, params: Set[PartParam], cacheGroups: Set[CacheGroup]): HttpPartConfig = originalPart.copy(
      partId = data.partId,
      parameters = params,
      uriToInterpolate = data.uri,
      description = data.description,
      method = HttpMethod.withName(data.method),
      additionalValidStatuses = HttpPartConfig.parseValidStatuses(data.additionalValidStatuses.filterNot(_.isEmpty)),
      hystrixConfig = Some(originalPart.hystrixConfigItem.copy(
        commandKey = data.commandKey,
        commandGroupKey = data.commandGroupKey,
        threadPoolConfigId = Some(data.threadPoolConfigId),
        timeoutInMs = data.timeoutInMs,
        updatedAt = DateTime.now
      )),
      deprecatedInFavourOf = data.deprecatedTo,
      cacheTtl = data.ttl.map(_.seconds),
      cacheGroups = cacheGroups,
      alertMailsEnabled = data.alertMailsEnabled,
      alertInterval = data.alertInterval.getOrElse(60L).seconds,
      alertAbsoluteThreshold = data.alertAbsoluteThreshold,
      alertPercentThreshold = data.alertPercentThreshold.map(_.toDouble),
      alertMailRecipients = data.alertMailRecipients,
      updatedAt = DateTime.now
    )

  }

  object PartData {

    def fromHttpPartConfig(part: HttpPartConfig) = PartData(
      partId = part.partId,
      description = part.description,
      deprecatedTo = part.deprecatedInFavourOf,
      uri = part.uriToInterpolate,
      method = part.method.toString,
      additionalValidStatuses = Some(part.additionalValidStatuses.mkString(",")).filterNot(_.isEmpty),
      commandKey = part.hystrixConfigItem.commandKey,
      commandGroupKey = part.hystrixConfigItem.commandGroupKey,
      timeoutInMs = part.hystrixConfigItem.timeoutInMs,
      threadPoolConfigId = part.hystrixConfigItem.threadPoolConfigId.get,
      ttl = part.cacheTtl.map(_.toSeconds),
      cacheGroupNames = part.cacheGroups.map(_.name).toSeq,
      alertMailsEnabled = part.alertMailsEnabled,
      alertInterval = Some(part.alertInterval.toSeconds),
      alertAbsoluteThreshold = part.alertAbsoluteThreshold,
      alertPercentThreshold = part.alertPercentThreshold.map(BigDecimal(_)),
      alertMailRecipients = part.alertMailRecipients
    )

  }

  val partForm = Form(
    mapping(
      "partId" -> text,
      "description" -> optional(text),
      "deprecatedTo" -> optional(text),
      "uri" -> text,
      "method" -> text.verifying(string => HttpMethod.values.exists(_.toString == string)),
      "additionalValidStatuses" -> optional(text),
      "commandKey" -> text,
      "commandGroupKey" -> text,
      "timeoutInMs" -> longNumber,
      "threadPoolConfigId" -> longNumber,
      "ttl" -> optional(longNumber),
      "cacheGroupNames" -> seq(text),
      "alertMailsEnabled" -> boolean,
      "alertInterval" -> optional(longNumber),
      "alertAbsoluteThreshold" -> optional(number),
      "alertPercentThreshold" -> optional(bigDecimal),
      "alertMailRecipients" -> optional(text)
    )(PartData.apply)(PartData.unapply)
  )

  case class ParamData(
    outputName: String,
    inputNameOverride: Option[String],
    description: Option[String],
    paramType: String,
    required: Boolean,
    versioned: Boolean,
    cacheGroupNames: Seq[String])

  val paramForm = Form(
    mapping(
      "outputName" -> text,
      "inputNameOverride" -> optional(text),
      "description" -> optional(text),
      "paramType" -> text.verifying(string => ParamType.values.exists(_.toString == string)),
      "required" -> boolean,
      "versioned" -> boolean,
      "cacheGroups" -> seq(text)
    )(ParamData.apply)(ParamData.unapply)
  )

  case class ThreadPoolData(threadPoolKey: String, coreSize: Int)

  val threadPoolForm = Form(
    mapping(
      "threadPoolKey" -> text,
      "coreSize" -> number
    )(ThreadPoolData.apply)(ThreadPoolData.unapply)
  )

  case class CacheGroupData(name: String, description: String)

  val cacheGroupForm = Form(
    mapping(
      "name" -> text,
      "description" -> text
    )(CacheGroupData.apply)(CacheGroupData.unapply)
  )

}
