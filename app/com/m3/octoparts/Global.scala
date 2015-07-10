package com.m3.octoparts

import java.io.File
import java.util.concurrent.TimeUnit

import _root_.controllers.ControllersModule
import com.beachape.zipkin.ZipkinHeaderFilter
import com.beachape.zipkin.services.ZipkinServiceLike
import com.kenshoo.play.metrics.MetricsFilter
import com.m3.octoparts.cache.CacheModule
import com.m3.octoparts.http.HttpModule
import com.m3.octoparts.hystrix.{ KeyAndBuilderValuesHystrixPropertiesStrategy, HystrixMetricsLogger, HystrixModule }
import com.m3.octoparts.logging.PartRequestLogger
import com.beachape.logging.LTSVLogger
import com.m3.octoparts.repository.{ ConfigsRepository, RepositoriesModule }
import com.netflix.hystrix.strategy.HystrixPlugins
import com.twitter.zipkin.gen.Span
import com.typesafe.config.ConfigFactory
import com.wordnik.swagger.config.{ ConfigFactory => SwaggerConfigFactory }
import com.wordnik.swagger.model.ApiInfo
import org.apache.commons.lang3.StringUtils
import play.api._
import play.api.libs.concurrent.Akka
import play.api.mvc._
import scaldi.Module
import scaldi.play.ScaldiSupport

import scala.concurrent.duration._
import scala.util.control.NonFatal

object Global extends WithFilters(ZipkinHeaderFilter(ZipkinServiceHolder.ZipkinService), MetricsFilter) with ScaldiSupport {

  val info = ApiInfo(
    title = "Octoparts",
    description = """Octoparts is an API aggregator service for your backend HTTP services.""",
    termsOfServiceUrl = "<Choose your own terms of service url>",
    contact = "<Put your own contact info here>",
    license = "<Choose your own licence>",
    licenseUrl = "<Choose your own licence URL>")

  SwaggerConfigFactory.config.setApiInfo(info)

  def applicationModule =
    aggregator.module ::
      new RepositoriesModule ::
      new CacheModule ::
      new HystrixModule ::
      new HttpModule ::
      new ControllersModule ::
      new Module {
        // Random stuff that doesn't belong in other modules
        bind[PartRequestLogger] to PartRequestLogger
        bind[ZipkinServiceLike] to ZipkinServiceHolder.ZipkinService
      }

  // Load environment-specific application.${env}.conf, merged with the generic application.conf
  override def onLoadConfig(config: Configuration, path: File, classloader: ClassLoader, mode: Mode.Mode): Configuration = {
    val playEnv = config.getString("application.env").fold(mode.toString) { parsedEnv =>
      // "test" mode should cause the environment to be "test" except when the parsedEnv is "ci",
      // since CI/Jenkins needs its own test environment configuration
      (mode.toString.toLowerCase, parsedEnv.toLowerCase) match {
        case ("test", env) if env != "ci" => "test"
        case (_, env) => env
      }
    }
    LTSVLogger.debug("Play environment" -> playEnv, "mode" -> mode, "application.env" -> config.getString("application.env"), "message" -> "Loading extra config...")
    val modeSpecificConfig = config ++ Configuration(ConfigFactory.load(s"application.$playEnv.conf"))
    super.onLoadConfig(modeSpecificConfig, path, classloader, mode)
  }

  override def onStart(app: Application) = {
    // Need to do this as early as possible, before Hystrix gets instantiated
    setHystrixPropertiesStrategy()

    super.onStart(app)

    startPeriodicTasks(app)
    checkForDodgyPartIds()
  }

  /**
   * Register any tasks that should be run on the global Akka scheduler.
   * These tasks will automatically stop running when the app shuts down.
   */
  private def startPeriodicTasks(implicit app: Application): Unit = {
    import play.api.libs.concurrent.Execution.Implicits.defaultContext

    val hystrixLoggingInterval = app.configuration.underlying.getDuration("hystrix.logging.intervalMs", TimeUnit.MILLISECONDS).toInt.millis
    Akka.system.scheduler.schedule(hystrixLoggingInterval, hystrixLoggingInterval) {
      HystrixMetricsLogger.logHystrixMetrics()
    }
  }

  /**
   * Check if there are any registered parts with leading/trailing spaces in their partIds.
   * Output warning logs if we find any, as they can be a nightmare to debug and are best avoided.
   */
  private def checkForDodgyPartIds(): Unit = {
    import play.api.libs.concurrent.Execution.Implicits.defaultContext
    implicit val emptySpan = new Span() // empty span -> doesn't trace
    val configsRepo = inject[ConfigsRepository]
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

  /**
   * Tries to set the Hystrix properties strategy to [[KeyAndBuilderValuesHystrixPropertiesStrategy]]
   *
   * Resist the temptation to do a HystrixPlugins.getInstance().getPropertiesStrategy first to do
   * checking, as that actually also sets the strategy if it isn't already set.
   */
  def setHystrixPropertiesStrategy(): Unit = {
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
