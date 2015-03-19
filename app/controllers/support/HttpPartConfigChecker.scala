package controllers.support

import java.util.UUID

import com.m3.octoparts.aggregator.handler.HttpPartRequestHandler
import com.m3.octoparts.model.config.{ HttpPartConfig, ParamType, PartParam }
import com.netaporter.uri.dsl._
import play.api.i18n.{ Lang, Messages }
import play.api.{ Application, Mode }

trait HttpPartConfigChecker {
  def warnings(t: HttpPartConfig)(implicit lang: Lang): Seq[String]
}

object HttpPartConfigChecker {
  def apply(httpPartConfig: HttpPartConfig)(implicit lang: Lang, app: Application): Seq[String] = Seq(QueryParamInterpolation, MissingPathParam, PathParamNoInterp, PathParamOption, WhitespaceInUri, AlertEmailOff(app.mode)).flatMap(_.warnings(httpPartConfig))
}

object QueryParamInterpolation extends HttpPartConfigChecker {
  def warnings(hpc: HttpPartConfig)(implicit lang: Lang): Seq[String] = {
    val someValue = UUID.randomUUID().toString
    val interpolated = HttpPartRequestHandler.interpolate(hpc.uriToInterpolate)(_ => someValue)
    interpolated.query.params.collect {
      case param if param._2.contains(someValue) => Messages("admin.warnings.QueryParamInterpolation", param._1)
    }
  }
}

/**
 * Prints warning when there is a missing interpolation token found
 */
object MissingPathParam extends HttpPartConfigChecker {
  def warnings(hpc: HttpPartConfig)(implicit lang: Lang): Seq[String] = {
    val missingMark = UUID.randomUUID().toString
    val foundMark = UUID.randomUUID().toString
    val pathParamValues = hpc.parameters.toSeq.collect {
      case pp: PartParam if pp.paramType == ParamType.Path => pp.outputName -> foundMark
    }.toMap
    val interpolated = HttpPartRequestHandler.interpolate(hpc.uriToInterpolate) { from: String => pathParamValues.getOrElse(from, s"$missingMark$from$missingMark") }

    def missingParameters(in: String, from: Int = 0): List[String] = {
      in.indexOf(missingMark, from) match {
        case -1 => Nil
        case found => {
          val end = in.indexOf(missingMark, found + missingMark.length)
          in.substring(found + missingMark.length, end) :: missingParameters(in, end + missingMark.length)
        }
      }
    }
    missingParameters(interpolated.path).map(Messages("admin.warnings.MissingPathParam", _))
  }
}

/**
 * Prints warning when there is a path param with no interpolation
 */
object PathParamNoInterp extends HttpPartConfigChecker {
  def warnings(hpc: HttpPartConfig)(implicit lang: Lang): Seq[String] = {
    hpc.parameters.toSeq.collect {
      case pp: PartParam if pp.paramType == ParamType.Path && !hpc.uriToInterpolate.containsSlice(s"$${${pp.outputName}}") => pp.outputName
    }.map(Messages("admin.warnings.PathParamNoInterp", _))
  }
}

/**
 * Prints warning when there is an optional path param
 */
object PathParamOption extends HttpPartConfigChecker {
  def warnings(hpc: HttpPartConfig)(implicit lang: Lang): Seq[String] = {
    hpc.parameters.toSeq.collect {
      case pp: PartParam if pp.paramType == ParamType.Path && !pp.required => pp.outputName
    }.map(Messages("admin.warnings.PathParamOption", _))
  }
}

/**
 * Prints a warning when there is a whitespace in the URI
 */
object WhitespaceInUri extends HttpPartConfigChecker {
  def warnings(hpc: HttpPartConfig)(implicit lang: Lang): Seq[String] = {
    if (hpc.uriToInterpolate.filter(Character.isWhitespace).isEmpty) Nil
    else Seq(Messages("admin.warnings.WhitespaceInUri"))
  }
}

/**
 * Prints a warning when email alerts are disabled in production
 */
case class AlertEmailOff(mode: Mode.Mode) extends HttpPartConfigChecker {
  def warnings(hpc: HttpPartConfig)(implicit lang: Lang): Seq[String] = {
    if (mode == Mode.Prod && !hpc.alertMailsEnabled) Seq(Messages("admin.warnings.AlertEmailOff"))
    else Nil
  }
}

