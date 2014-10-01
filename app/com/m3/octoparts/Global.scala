package com.m3.octoparts

import java.io.File
import java.util.concurrent.TimeUnit

import _root_.controllers.ControllersModule
import com.kenshoo.play.metrics.MetricsFilter
import com.m3.octoparts.cache.CacheModule
import com.m3.octoparts.http.HttpModule
import com.m3.octoparts.hystrix.{ CachelessHystrixPropertiesStrategy, HystrixMetricsLogger, HystrixModule }
import com.m3.octoparts.logging.PartRequestLogger
import com.beachape.logging.LTSVLogger
import com.m3.octoparts.repository.RepositoriesModule
import com.netflix.hystrix.strategy.HystrixPlugins
import com.typesafe.config.ConfigFactory
import com.wordnik.swagger.config.{ ConfigFactory => SwaggerConfigFactory }
import com.wordnik.swagger.model.ApiInfo
import play.api._
import play.api.libs.concurrent.Akka
import play.api.mvc._
import scaldi.Module
import scaldi.play.ScaldiSupport

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration._
import scala.util.control.NonFatal

object Global extends WithFilters(MetricsFilter) with ScaldiSupport {

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
      }

  /**
   * For each entry, V.getClass == K
   */
  private val controllerCache = TrieMap[Class[_], Any]()

  /**
   * Caches controller instantiation which was shown to be expensive because of ScalDI.
   */
  override def getControllerInstance[A](clazz: Class[A]): A = {
    controllerCache.getOrElseUpdate(clazz, super.getControllerInstance(clazz)).asInstanceOf[A]
  }

  override def onStop(app: Application) = {
    controllerCache.clear()
    super.onStop(app)
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
    setHystrixPropertiesStrategy(app)
    super.onStart(app)
    startPeriodicTasks(app)
  }

  /**
   * Register any tasks that should be run on the global Akka scheduler.
   * These tasks will automatically stop running when the app shuts down.
   */
  def startPeriodicTasks(implicit app: Application): Unit = {
    import play.api.libs.concurrent.Execution.Implicits.defaultContext

    val hystrixLoggingInterval = app.configuration.underlying.getDuration("hystrix.logging.intervalMs", TimeUnit.MILLISECONDS).toInt.millis
    Akka.system.scheduler.schedule(hystrixLoggingInterval, hystrixLoggingInterval) {
      HystrixMetricsLogger.logHystrixMetrics()
    }
  }

  /**
   * Tries to set the Hystrix properties strategy to CachelessHystrixPropertiesStrategy
   *
   * Resist the temptation to do a HystrixPlugins.getInstance().getPropertiesStrategy first to do
   * checking, as that actually also sets the strategy if it isn't already set.
   */
  def setHystrixPropertiesStrategy(app: Application): Unit = {
    // If it's defined, we don't need to set anything
    if (sys.props.get("hystrix.plugin.HystrixPropertiesStrategy.implementation").isEmpty) {
      LTSVLogger.warn("-Dhystrix.plugin.HystrixPropertiesStrategy.implementation is not set. It should be" -> "com.m3.octoparts.hystrix.CachelessHystrixPropertiesStrategy")
      try {
        HystrixPlugins.getInstance().registerPropertiesStrategy(new CachelessHystrixPropertiesStrategy)
      } catch {
        case NonFatal(e) => {
          val currentStrategy = HystrixPlugins.getInstance().getPropertiesStrategy.getClass
          if (currentStrategy != classOf[CachelessHystrixPropertiesStrategy]) {
            LTSVLogger.error(e, "Current Hystrix Properties Strategy:" -> currentStrategy)
            if (Play.mode(app) != Mode.Test)
              sys.error("Tried but failed to set CachelessHystrixPropertiesStrategy as HystrixPropertiesStrategy")
          }
        }
      }
    }
  }
}
