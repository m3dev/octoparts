package controllers.system

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.status.StatusUtil
import ch.qos.logback.core.util.StatusPrinter
import com.typesafe.config.{ ConfigRenderOptions, ConfigValueFactory, Config => TSConfig }
import org.slf4j.LoggerFactory
import play.api.mvc.{ Action, Controller }

import scala.collection.convert.Wrappers.{ JListWrapper, JSetWrapper }

/**
 * A controller that pretty-prints the Typesafe config that the app is using.
 *
 * This is the config calculated from merging the defaults with any environment-specific overrides.
 * It has all system properties and environment variables resolved to their appropriate variables.
 */
class SystemConfigController(config: TSConfig) extends Controller {

  def showSystemConfig = Action { request =>
    val maskedConfig = maskPasswords(config)
    Ok(maskedConfig.resolve().root().render(ConfigRenderOptions.concise().setFormatted(true))).as(JSON)
  }

  // prints the status of the logger to the response
  def showLoggerStatus = Action { request =>
    val statusManager = LoggerFactory.getILoggerFactory match {
      case lc: LoggerContext => lc.getStatusManager
      case other => throw new IllegalStateException(s"Invalid logger factory: $other")
    }
    // copied from StatusPrinter, because it has a silly design and would keep a reference to the response output stream.
    val sb = new java.lang.StringBuilder
    val statusList = StatusUtil.filterStatusListByTimeThreshold(statusManager.getCopyOfStatusList, 0)
    for (s <- JListWrapper(statusList)) {
      StatusPrinter.buildStr(sb, "", s)
    }
    Ok(sb.toString).as(TEXT)
  }

  private def maskPasswords(config: TSConfig): TSConfig = {
    val passwordPaths = JSetWrapper(config.entrySet).collect {
      case entry: java.util.Map.Entry[String, _] if entry.getKey.split('.').last == "password" => entry.getKey
    }
    passwordPaths.foldLeft(config) { (cfg, path) => cfg.withValue(path, ConfigValueFactory.fromAnyRef("****", "masked password")) }
  }

}

