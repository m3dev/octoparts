package controllers.system

import com.m3.octoparts.BuildInfo
import org.joda.time.{ DateTimeZone, LocalDateTime }
import play.api.libs.json.{ JsArray, JsString, Json }
import play.api.mvc.{ AbstractController, Action, Controller, ControllerComponents }

/**
 * A controller that simply prints the BuildInfo object as json
 */
class BuildInfoController(controllerComponents: ControllerComponents) extends AbstractController(controllerComponents) {

  def showBuildInfo = Action {
    val json = Json.obj(
      "version" -> BuildInfo.version,
      "scala_version" -> BuildInfo.scalaVersion,
      "build_time_millis" -> BuildInfo.buildTime,
      "build_time_pretty" -> new LocalDateTime(BuildInfo.buildTime, DateTimeZone.forID("Asia/Tokyo")).toString("yyyy-MM-dd HH:mm:ss"),
      "git_branch" -> BuildInfo.gitBranch,
      "git_tags" -> JsArray(BuildInfo.gitTags.map(JsString(_))),
      "git_head" -> JsString(BuildInfo.gitHEAD getOrElse "<unknown>")
    )
    Ok(json)
  }

}

