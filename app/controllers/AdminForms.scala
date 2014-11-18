package controllers

import java.nio.charset.Charset

import com.beachape.logging.LTSVLogger
import com.m3.octoparts.model.HttpMethod
import com.m3.octoparts.model.config._
import org.apache.commons.lang3.StringUtils
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.data._

import scala.concurrent.duration._

object AdminForms {

  case class AlertMailData(enabled: Boolean,
                           interval: Option[Int],
                           absoluteThreshold: Option[Int],
                           percentThreshold: Option[BigDecimal],
                           recipients: Option[String])

  case class PartData(
      partId: String,
      description: Option[String],
      deprecatedTo: Option[String],
      uri: String,
      method: String,
      additionalValidStatuses: Option[String],
      httpPoolSize: Int,
      httpConnectionTimeoutInMs: Int,
      httpSocketTimeoutInMs: Int,
      httpDefaultEncoding: String,
      commandKey: String,
      commandGroupKey: String,
      timeoutInMs: Int,
      threadPoolConfigId: Long,
      ttl: Option[Int],
      cacheGroupNames: Seq[String],
      alertMailData: AlertMailData) {
    data =>

    /** Create a brand new HttpPartConfig using the data input into the form */
    def toNewHttpPartConfig(owner: String, cacheGroups: Set[CacheGroup]): HttpPartConfig = new HttpPartConfig(
      partId = PartData.trimPartId(data.partId),
      owner = owner,
      uriToInterpolate = data.uri,
      description = data.description,
      method = HttpMethod.withName(data.method),
      additionalValidStatuses = HttpPartConfig.parseValidStatuses(data.additionalValidStatuses.filterNot(_.isEmpty)),
      httpPoolSize = data.httpPoolSize,
      httpConnectionTimeout = data.httpConnectionTimeoutInMs.milliseconds,
      httpSocketTimeout = data.httpSocketTimeoutInMs.milliseconds,
      httpDefaultEncoding = Charset.forName(data.httpDefaultEncoding),
      hystrixConfig = Some(new HystrixConfig(
        commandKey = data.commandKey,
        commandGroupKey = data.commandGroupKey,
        threadPoolConfigId = Some(data.threadPoolConfigId),
        timeoutInMs = data.timeoutInMs.milliseconds,
        createdAt = DateTime.now,
        updatedAt = DateTime.now
      )),
      deprecatedInFavourOf = data.deprecatedTo,
      cacheTtl = data.ttl.map(_.seconds),
      cacheGroups = cacheGroups,
      alertMailsEnabled = alertMailData.enabled,
      alertInterval = alertMailData.interval.map(_.seconds).getOrElse(1.minute),
      alertAbsoluteThreshold = alertMailData.absoluteThreshold,
      alertPercentThreshold = alertMailData.percentThreshold.map(_.toDouble),
      alertMailRecipients = alertMailData.recipients,
      createdAt = DateTime.now,
      updatedAt = DateTime.now
    )

    /** Update an existing HttpPartConfig using the data input into the form */
    def toUpdatedHttpPartConfig(originalPart: HttpPartConfig, params: Set[PartParam], cacheGroups: Set[CacheGroup]): HttpPartConfig = originalPart.copy(
      partId = PartData.trimPartId(data.partId),
      parameters = params,
      uriToInterpolate = data.uri,
      description = data.description,
      method = HttpMethod.withName(data.method),
      additionalValidStatuses = HttpPartConfig.parseValidStatuses(data.additionalValidStatuses.filterNot(_.isEmpty)),
      httpPoolSize = data.httpPoolSize,
      httpConnectionTimeout = data.httpConnectionTimeoutInMs.milliseconds,
      httpSocketTimeout = data.httpSocketTimeoutInMs.milliseconds,
      httpDefaultEncoding = Charset.forName(data.httpDefaultEncoding),
      hystrixConfig = Some(originalPart.hystrixConfigItem.copy(
        commandKey = data.commandKey,
        commandGroupKey = data.commandGroupKey,
        threadPoolConfigId = Some(data.threadPoolConfigId),
        timeoutInMs = data.timeoutInMs.milliseconds,
        updatedAt = DateTime.now
      )),
      deprecatedInFavourOf = data.deprecatedTo,
      cacheTtl = data.ttl.map(_.seconds),
      cacheGroups = cacheGroups,
      alertMailsEnabled = alertMailData.enabled,
      alertInterval = alertMailData.interval.map(_.seconds).getOrElse(1.minute),
      alertAbsoluteThreshold = alertMailData.absoluteThreshold,
      alertPercentThreshold = alertMailData.percentThreshold.map(_.toDouble),
      alertMailRecipients = alertMailData.recipients,
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
      httpPoolSize = part.httpPoolSize,
      httpConnectionTimeoutInMs = part.httpConnectionTimeout.toMillis.toInt,
      httpSocketTimeoutInMs = part.httpSocketTimeout.toMillis.toInt,
      httpDefaultEncoding = part.httpDefaultEncoding.name(),
      commandKey = part.hystrixConfigItem.commandKey,
      commandGroupKey = part.hystrixConfigItem.commandGroupKey,
      timeoutInMs = part.hystrixConfigItem.timeoutInMs.toMillis.toInt,
      threadPoolConfigId = part.hystrixConfigItem.threadPoolConfigId.get,
      ttl = part.cacheTtl.map(_.toSeconds.toInt),
      cacheGroupNames = part.cacheGroups.map(_.name).toSeq,
      alertMailData = AlertMailData(
        enabled = part.alertMailsEnabled,
        interval = Some(part.alertInterval.toSeconds.toInt),
        absoluteThreshold = part.alertAbsoluteThreshold,
        percentThreshold = part.alertPercentThreshold.map(BigDecimal(_)),
        recipients = part.alertMailRecipients
      )
    )

    private def trimPartId(original: String): String = {
      val trimmed = StringUtils.strip(original)
      if (trimmed != original) {
        LTSVLogger.info("message" -> "Leading and trailing spaces were trimmed from partId", "before" -> s"'$original'", "after" -> s"'$trimmed'")
      }
      trimmed
    }

  }

  val partForm = Form(
    mapping(
      "partId" -> text,
      "description" -> optional(text),
      "deprecatedTo" -> optional(text),
      "uri" -> text,
      "method" -> text.verifying(string => HttpMethod.values.exists(_.toString == string)),
      "additionalValidStatuses" -> optional(text),
      "httpPoolSize" -> number(min = 1),
      "httpConnectionTimeoutInMs" -> number(min = 0),
      "httpSocketTimeoutInMs" -> number(min = 0),
      "httpDefaultEncoding" -> text.verifying(string => Charset.isSupported(string)),
      "commandKey" -> text,
      "commandGroupKey" -> text,
      "timeoutInMs" -> number(min = 0),
      "threadPoolConfigId" -> longNumber,
      "ttl" -> optional(number(min = 0)),
      "cacheGroupNames" -> seq(text),
      "alertMail" -> mapping(
        "enabled" -> boolean,
        "interval" -> optional(number(min = 1)),
        "absoluteThreshold" -> optional(number(min = 1)),
        "percentThreshold" -> optional(bigDecimal),
        "recipients" -> optional(text)
      )(AlertMailData.apply)(AlertMailData.unapply)
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
