package presentation

import com.m3.octoparts.model.config.HttpPartConfig
import controllers.support.HttpPartConfigChecker
import org.apache.commons.lang.StringEscapeUtils
import org.joda.time.DateTime
import play.api.Play
import play.api.i18n.{ Lang, Messages }
import play.twirl.api.Html

import scala.collection.SortedSet
import scala.concurrent.duration.Duration

/**
 * View adapter for an HttpPartConfig.
 */
case class HttpPartConfigView(config: HttpPartConfig)(implicit messages: Messages) {

  def addParamLink: String = controllers.routes.AdminController.newParam(config.partId).url

  def detailLink: String = controllers.routes.AdminController.showPart(config.partId).url

  def tryItLink: String = controllers.routes.AdminController.testPart(config.partId).url

  def editLink: String = controllers.routes.AdminController.editPart(config.partId).url

  def exportLink: String = controllers.routes.PartsController.list(List(partId)).url

  def commandGroup: Option[String] = config.hystrixConfig.map(_.commandGroupKey)

  lazy val warnings: Seq[String] = HttpPartConfigChecker(config)

  def timeoutInMs: Int = config.hystrixConfig.fold(5000)(_.timeout.toMillis.toInt)

  def commandKey: Option[String] = config.hystrixConfig.map(_.commandKey)

  def uriToInterpolate: String = config.uriToInterpolate

  def partId: String = config.partId

  def partIdJs: String = StringEscapeUtils.escapeJavaScript(partId)

  def httpMethod: String = config.method.toString

  def threadPoolKey: Option[String] = config.hystrixConfig.flatMap(_.threadPoolConfig).map(_.threadPoolKey)

  def registeredParamsView: SortedSet[ParamView] = config.parameters.map(ParamView.apply)

  def editableParamsView: SortedSet[ParamView] = registeredParamsView.filterNot(_.name.startsWith("meta."))

  def description: Option[String] = config.description

  def deprecation: Html = config.deprecatedInFavourOf match {
    case Some(s) if s.length() > 0 =>
      Html(Messages(
        "parts.deprecation.seeOther", s"""<a href="${controllers.routes.AdminController.showPart(s).url}">$s</a>"""
      ))
    case _ => Html(Messages("parts.deprecation.none"))
  }

  def lastUpdate: String = formatTs(config.updatedAt)

  def created: String = formatTs(config.createdAt)

  def cacheTtlStr: String = {
    config.cacheTtl.fold(Messages("parts.cache.unlimited")) {
      case Duration.Zero => Messages("parts.cache.none")
      case dd => dd.toString() // not worth localizing yet
    }
  }

  def alertMailCondition: Html = {
    val thresholdCondition = (
      config.alertAbsoluteThreshold.map(Messages("parts.alertMail.condition.absolute", _)) ++
      config.alertPercentThreshold.map(Messages("parts.alertMail.condition.relative", _))
    ).mkString(s" ${Messages("parts.alertMail.condition.or")} ")
    Html(Messages("parts.alertMail.condition.summary", config.alertInterval.toString(), thresholdCondition))
  }

  def alertMailRecipients: String = config.alertMailRecipients.getOrElse(Messages("parts.alertMail.recipients.none"))

  def additionalValidStatuses: String = config.additionalValidStatuses.mkString(", ")

  private def formatTs(ts: DateTime) = Option(ts).fold(Messages("na")) {
    _.toString(Messages("ymdFormat"))
  }

}

object HttpPartConfigView {
  implicit val order: Ordering[HttpPartConfigView] = Ordering.by(_.config)
}
