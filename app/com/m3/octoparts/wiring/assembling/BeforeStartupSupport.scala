package com.m3.octoparts.wiring.assembling

import java.util.concurrent.TimeUnit

import com.beachape.logging.LTSVLogger
import com.m3.octoparts.hystrix.{ KeyAndBuilderValuesHystrixPropertiesStrategy, HystrixMetricsLogger }
import com.netflix.hystrix.strategy.HystrixPlugins
import com.twitter.zipkin.gen.Span
import org.apache.commons.lang3.StringUtils
import org.flywaydb.play.{ PlayInitializer => FlywayPlayInitializer }
import pl.matisoft.swagger.{ SwaggerPluginProvider, SwaggerModule }
import scalikejdbc.PlayInitializer

import scala.util.control.NonFatal
import scala.concurrent.duration._

trait BeforeStartupSupport extends SwaggerScanSupport {

  protected def beforeStart(components: ApplicationComponents): Unit = {
    // Need to initialise the DB first before anything else is run
    initDB(components)

    setHystrixPropertiesStrategy()
    initSwagger(components)
    startPeriodicTasks(components)
    checkForDodgyParts(components)
  }

  /**
   * Register any tasks that should be run on the global Akka scheduler.
   * These tasks will automatically stop running when the app shuts down.
   */
  private def startPeriodicTasks(components: ApplicationComponents): Unit = {
    val config = components.configuration
    val actorSystem = components.actorSystem
    implicit val context = actorSystem.dispatcher
    val hystrixLoggingInterval = config.underlying.getDuration("hystrix.logging.intervalMs", TimeUnit.MILLISECONDS).toInt.millis
    actorSystem.scheduler.schedule(hystrixLoggingInterval, hystrixLoggingInterval) {
      HystrixMetricsLogger.logHystrixMetrics()
    }
  }

  /**
   * Check if there are any registered parts with leading/trailing spaces in their partIds.
   * Output warning logs if we find any, as they can be a nightmare to debug and are best avoided.
   */
  private def checkForDodgyParts(components: ApplicationComponents): Unit = {
    import play.api.libs.concurrent.Execution.Implicits.defaultContext
    implicit val emptySpan = new Span() // empty span -> doesn't trace
    val configsRepo = components.configsRepository
    for {
      configs <- configsRepo.findAllConfigs()
      config <- configs
    } {
      val trimmed = StringUtils.strip(config.partId)
      if (trimmed != config.partId) {
        LTSVLogger.warn("message" -> "This partId is suspicious - it has leading/trailing spaces", "partId" -> s"'${config.partId}'")
      }
    }
  }

  private def initDB(components: ApplicationComponents): Unit = {
    val lifecycle = components.applicationLifecycle
    val config = components.configuration
    val env = components.environment
    val webcommands = components.webCommands
    new PlayInitializer(lifecycle, config)
    new FlywayPlayInitializer(config, env, webcommands)
  }

  /**
   * Tries to set the Hystrix properties strategy to [[KeyAndBuilderValuesHystrixPropertiesStrategy]]
   *
   * Resist the temptation to do a HystrixPlugins.getInstance().getPropertiesStrategy first to do
   * checking, as that actually also sets the strategy if it isn't already set.
   */
  private[octoparts] def setHystrixPropertiesStrategy(): Unit = {
    // If it's defined, we don't need to set anything
    if (sys.props.get("hystrix.plugin.HystrixPropertiesStrategy.implementation").isEmpty) {
      LTSVLogger.info("-Dhystrix.plugin.HystrixPropertiesStrategy.implementation is not set. Defaulting to" -> "com.m3.octoparts.hystrix.KeyAndBuilderValuesHystrixPropertiesStrategy")
      try {
        HystrixPlugins.getInstance().registerPropertiesStrategy(new KeyAndBuilderValuesHystrixPropertiesStrategy)
      } catch {
        case NonFatal(e) =>
          val currentStrategy = HystrixPlugins.getInstance().getPropertiesStrategy.getClass
          LTSVLogger.info(e, "Current Hystrix Properties Strategy:" -> currentStrategy)
      }
    }
  }

}
