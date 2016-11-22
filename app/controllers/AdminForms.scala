package controllers

import java.nio.charset.{ Charset => JavaCharset }
import com.beachape.logging.LTSVLogger
import com.m3.octoparts.model.HttpMethod
import com.m3.octoparts.model.config._
import org.apache.commons.lang3.StringUtils
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.data._

import scala.collection.SortedSet
import scala.concurrent.duration._

object AdminForms {

  case class AlertMailData(
    enabled: Boolean,
    interval: Option[Int],
    absoluteThreshold: Option[Int],
    percentThreshold: Option[BigDecimal],
    recipients: Option[String]
  )

  case class PartData(
      partId: String,
      description: Option[String],
      deprecatedTo: Option[String],
      httpSettings: HttpConfigData,
      hystrixConfig: HystrixConfigData,
      ttl: Option[Int],
      cacheGroupNames: Seq[String],
      alertMailData: AlertMailData,
      localContentsConfig: LocalContentsConfig
  ) {
    data =>

    /** Create a brand new HttpPartConfig using the data input into the form */
    def toNewHttpPartConfig(
      owner: String,
      cacheGroups: SortedSet[CacheGroup]
    ): HttpPartConfig = HttpPartConfig(
      partId = PartData.trimPartId(data.partId),
      owner = owner,
      description = data.description,
      uriToInterpolate = data.httpSettings.uri,
      method = HttpMethod.withName(data.httpSettings.method),
      additionalValidStatuses = HttpPartConfig.parseValidStatuses(data.httpSettings.additionalValidStatuses),
      httpPoolSize = data.httpSettings.httpPoolSize,
      httpConnectionTimeout = data.httpSettings.httpConnectionTimeoutInMs.milliseconds,
      httpSocketTimeout = data.httpSettings.httpSocketTimeoutInMs.milliseconds,
      httpDefaultEncoding = Charset.forName(data.httpSettings.httpDefaultEncoding),
      httpProxy = data.httpSettings.httpProxy,
      hystrixConfig = Some(HystrixConfig(
        commandKey = data.hystrixConfig.commandKey,
        commandGroupKey = data.hystrixConfig.commandGroupKey,
        threadPoolConfigId = Some(data.hystrixConfig.threadPoolConfigId),
        timeout = data.hystrixConfig.timeoutInMs.milliseconds,
        localContentsAsFallback = data.hystrixConfig.localContentsAsFallback,
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
      localContentsEnabled = data.localContentsConfig.enabled,
      localContents = data.localContentsConfig.contents,
      createdAt = DateTime.now,
      updatedAt = DateTime.now
    )

    /** Update an existing HttpPartConfig using the data input into the form */
    def toUpdatedHttpPartConfig(
      originalPart: HttpPartConfig,
      cacheGroups: SortedSet[CacheGroup]
    ): HttpPartConfig = originalPart.copy(
      partId = PartData.trimPartId(data.partId),
      description = data.description,
      uriToInterpolate = data.httpSettings.uri,
      method = HttpMethod.withName(data.httpSettings.method),
      additionalValidStatuses = HttpPartConfig.parseValidStatuses(data.httpSettings.additionalValidStatuses),
      httpPoolSize = data.httpSettings.httpPoolSize,
      httpConnectionTimeout = data.httpSettings.httpConnectionTimeoutInMs.milliseconds,
      httpSocketTimeout = data.httpSettings.httpSocketTimeoutInMs.milliseconds,
      httpDefaultEncoding = Charset.forName(data.httpSettings.httpDefaultEncoding),
      httpProxy = data.httpSettings.httpProxy,
      hystrixConfig = Some(originalPart.hystrixConfigItem.copy(
        commandKey = data.hystrixConfig.commandKey,
        commandGroupKey = data.hystrixConfig.commandGroupKey,
        threadPoolConfigId = Some(data.hystrixConfig.threadPoolConfigId),
        localContentsAsFallback = data.hystrixConfig.localContentsAsFallback,
        timeout = data.hystrixConfig.timeoutInMs.milliseconds,
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
      localContentsEnabled = data.localContentsConfig.enabled,
      localContents = data.localContentsConfig.contents,
      updatedAt = DateTime.now
    )

  }

  object PartData {

    def fromHttpPartConfig(part: HttpPartConfig) = PartData(
      partId = part.partId,
      description = part.description,
      deprecatedTo = part.deprecatedInFavourOf,
      httpSettings = HttpConfigData(
        uri = part.uriToInterpolate,
        method = part.method.toString,
        additionalValidStatuses = Some(part.additionalValidStatuses.mkString(",")).filterNot(_.isEmpty),
        httpPoolSize = part.httpPoolSize,
        httpConnectionTimeoutInMs = part.httpConnectionTimeout.toMillis.toInt,
        httpSocketTimeoutInMs = part.httpSocketTimeout.toMillis.toInt,
        httpDefaultEncoding = part.httpDefaultEncoding.name,
        httpProxy = part.httpProxy
      ),
      hystrixConfig = HystrixConfigData(
        commandKey = part.hystrixConfigItem.commandKey,
        commandGroupKey = part.hystrixConfigItem.commandGroupKey,
        timeoutInMs = part.hystrixConfigItem.timeout.toMillis.toInt,
        threadPoolConfigId = part.hystrixConfigItem.threadPoolConfigId.get,
        localContentsAsFallback = part.hystrixConfigItem.localContentsAsFallback
      ),
      ttl = part.cacheTtl.map(_.toSeconds.toInt),
      cacheGroupNames = part.cacheGroups.toSeq.map(_.name),
      alertMailData = AlertMailData(
        enabled = part.alertMailsEnabled,
        interval = Some(part.alertInterval.toSeconds.toInt),
        absoluteThreshold = part.alertAbsoluteThreshold,
        percentThreshold = part.alertPercentThreshold.map(BigDecimal(_)),
        recipients = part.alertMailRecipients
      ),
      localContentsConfig = LocalContentsConfig(
        enabled = part.localContentsEnabled,
        contents = part.localContents
      )
    )

    private def trimPartId(original: String): String = {
      val trimmed = StringUtils.strip(original)
      if (trimmed != original) {
        LTSVLogger.info(
          "message" -> "Leading and trailing spaces were trimmed from partId",
          "before" -> s"'$original'",
          "after" -> s"'$trimmed'"
        )
      }
      trimmed
    }

  }

  val partForm = Form(
    mapping(
      "partId" -> text,
      "description" -> optional(text),
      "deprecatedTo" -> optional(text),
      "httpSettings" -> mapping(
        "uri" -> text,
        "method" -> text.verifying(string => HttpMethod.values.exists(_.toString == string)),
        "additionalValidStatuses" -> optional(text),
        "httpPoolSize" -> number(min = 1),
        "httpConnectionTimeoutInMs" -> number(min = 0),
        "httpSocketTimeoutInMs" -> number(min = 0),
        "httpDefaultEncoding" -> text.verifying(string => JavaCharset.isSupported(string)),
        "httpProxy" -> optional(text)
      )(HttpConfigData.apply)(HttpConfigData.unapply),
      "hystrixConfig" -> mapping(
        "commandKey" -> text,
        "commandGroupKey" -> text,
        "timeoutInMs" -> number(min = 0),
        "threadPoolConfigId" -> longNumber,
        "localContentsAsFallback" -> boolean
      )(HystrixConfigData.apply)(HystrixConfigData.unapply),
      "ttl" -> optional(number(min = 0)),
      "cacheGroupNames" -> seq(text),
      "alertMail" -> mapping(
        "enabled" -> boolean,
        "interval" -> optional(number(min = 1)),
        "absoluteThreshold" -> optional(number(min = 1)),
        "percentThreshold" -> optional(bigDecimal),
        "recipients" -> optional(text)
      )(AlertMailData.apply)(AlertMailData.unapply),
      "localContentsConfig" -> mapping(
        "enabled" -> boolean,
        "contents" -> optional(text)
      )(LocalContentsConfig.apply)(LocalContentsConfig.unapply)
    )(PartData.apply)(PartData.unapply)
  )

  case class LocalContentsConfig(
    enabled: Boolean,
    contents: Option[String]
  )

  case class HttpConfigData(
    uri: String,
    method: String,
    additionalValidStatuses: Option[String],
    httpPoolSize: Int,
    httpConnectionTimeoutInMs: Int,
    httpSocketTimeoutInMs: Int,
    httpDefaultEncoding: String,
    httpProxy: Option[String]
  )

  case class HystrixConfigData(
    commandKey: String,
    commandGroupKey: String,
    timeoutInMs: Int,
    threadPoolConfigId: Long,
    localContentsAsFallback: Boolean
  )

  case class ParamData(
    outputName: String,
    inputNameOverride: Option[String],
    description: Option[String],
    paramType: String,
    required: Boolean,
    versioned: Boolean,
    cacheGroupNames: Seq[String]
  )

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

  case class ThreadPoolData(threadPoolKey: String, coreSize: Int, queueSize: Int)

  val threadPoolForm = Form(
    mapping(
      "threadPoolKey" -> text,
      "coreSize" -> number(min = 1),
      "queueSize" -> number(min = -1) // Hystrix says that if -1 is used, it will use a synchronous queue
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
