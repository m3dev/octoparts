package presentation

import com.m3.octoparts.model.config.HttpPartConfig
import org.apache.commons.lang.StringEscapeUtils
import org.joda.time.DateTime
import play.api.i18n.{ Lang, Messages }
import play.twirl.api.Html

import scala.concurrent.duration.Duration

/**
 * View adapter for an HttpPartConfig.
 */
case class HttpPartConfigView(config: HttpPartConfig) {

  def addParamLink: String = controllers.routes.AdminController.newParam(config.partId).url

  def detailLink: String = controllers.routes.AdminController.showPart(config.partId).url

  def tryItLink: String = controllers.routes.AdminController.testPart(config.partId).url

  def editLink: String = controllers.routes.AdminController.editPart(config.partId).url

  def commandGroup: String = config.hystrixConfig.fold("")(_.commandGroupKey)

  def timeoutInMs: Long = config.hystrixConfig.fold(5000L)(_.timeoutInMs)

  def commandKey: String = config.hystrixConfig.fold("")(_.commandKey)

  def uriToInterpolate: String = config.uriToInterpolate

  def partId: String = config.partId

  def partIdJs: String = StringEscapeUtils.escapeJavaScript(partId)

  def httpMethod: String = config.method.toString

  def threadPoolKey: String = config.hystrixConfig.flatMap(_.threadPoolConfig).fold("")(_.threadPoolKey)

  def registeredParamsView: Set[ParamView] = config.parameters.map { p => ParamView(p) }

  def editableParamsView: Set[ParamView] = config.parameters.collect {
    case p if !p.inputName.startsWith("meta.") => ParamView(p)
  }

  def description: String = config.description

  def deprecation(implicit lang: Lang): Html = config.deprecatedInFavourOf match {
    case Some(s) if s.length() > 0 =>
      Html(Messages("parts.deprecation.seeOther", s"""<a href="${controllers.routes.AdminController.showPart(s).url}">$s</a>"""))
    case _ => Html(Messages("parts.deprecation.none"))
  }

  def lastUpdate: String = formatTs(config.updatedAt)

  def created: String = formatTs(config.createdAt)

  def cacheTtlStr(implicit lang: Lang): String = {
    config.cacheTtl.fold(Messages("parts.cache.unlimited")) {
      case Duration.Zero => Messages("parts.cache.none")
      case dd => dd.toString // not worth localizing yet
    }
  }

  def alertMailCondition(implicit lang: Lang): Html = {
    val thresholdCondition = (
      config.alertAbsoluteThreshold.map(Messages("parts.alertMail.condition.absolute", _)) ++
      config.alertPercentThreshold.map(Messages("parts.alertMail.condition.relative", _))
    ).mkString(s" ${Messages("parts.alertMail.condition.or")} ")
    Html(Messages("parts.alertMail.condition.summary", config.alertInterval.toString, thresholdCondition))
  }

  def alertMailRecipients(implicit lang: Lang): String = config.alertMailRecipients.getOrElse(Messages("parts.alertMail.recipients.none"))

  private def formatTs(ts: DateTime)(implicit lang: Lang) = Option(ts).fold(Messages("na")) {
    _.toString(Messages("ymdFormat"))
  }

}
