package controllers.system

import java.util.concurrent.TimeUnit

import com.beachape.logging.LTSVLogger
import com.m3.octoparts.cache.RawCache
import com.m3.octoparts.hystrix.HystrixHealthReporter
import com.m3.octoparts.logging.LogUtil
import com.m3.octoparts.repository.ConfigsRepository
import play.api.libs.json.{ Json, Writes }
import play.api.mvc.{ Action, Controller }
import shade.memcached.MemcachedCodecs

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.control.NonFatal

/**
 * A healthcheck API for use by Nagios.
 * Checks the status of the database and the Hystrix circuit breakers.
 */
class HealthcheckController(configsRepo: ConfigsRepository,
                            hystrixHealthReporter: HystrixHealthReporter,
                            memcached: RawCache,
                            memcachedCacheKeysToCheck: MemcachedCacheKeysToCheck) extends Controller with LogUtil {

  import controllers.system.HealthcheckController._
  import play.api.libs.concurrent.Execution.Implicits.defaultContext
  import MemcachedCodecs.StringBinaryCodec
  import com.m3.octoparts.future.RichFutureWithTiming._

  def healthcheck = Action.async { request =>
    val fDbStatus = checkDb()
    val fMemcachedStatus = checkMemcached()
    val hystrixStatus = checkHystrix()
    val fServiceHealth =
      for {
        dbStatus <- fDbStatus
        memcachedStatus <- fMemcachedStatus
      } yield {
        val statuses = Map[String, ServiceStatus](
          "db" -> dbStatus,
          "hystrix" -> hystrixStatus,
          "memcached" -> memcachedStatus
        )
        val colour = calculateColour(statuses.values)
        val health = ServiceHealth(colour, statuses)
        logIfUnhealthy(health)
        health
      }
    fServiceHealth.map(health => Ok(Json.toJson(health)(serviceHealthWrites)))
  }

  /**
   * Check that the DB connection is alive and there is at least one part registered in the system
   */
  private def checkDb(): Future[DbStatus] = {
    val fCount = configsRepo.findAllConfigs().map(_.length)
    fCount.map { count =>
      if (count > 0) DbStatus(ok = true, message = "DB looks fine")
      else DbStatus(ok = false, message = "parts_config table is empty!")
    }.recover {
      case NonFatal(e) =>
        LTSVLogger.warn(e, "Health check failed" -> "DB")
        DbStatus(ok = false, message = e.toString)
    } time { case(status, duration) =>  LTSVLogger.info("DbStatus" -> status, "Time taken" -> toRelevantUnit(duration)) }
  }

  /**
   * Check that Memcached is alive and responding to GET requests
   */
  private def checkMemcached(): Future[MemcachedStatus] = {
    Future.sequence {
      for (key <- memcachedCacheKeysToCheck()) yield {
        val memcachedGet = memcached.get[String](key)
        memcachedGet map { _ => true // Don't care whether we get a cache hit or not
        } recover {
          case NonFatal(e) =>
            LTSVLogger.warn(e, "Health check failed" -> "Memcached", "key" -> key)
            false
        }
      }
    } map {
      bools => MemcachedStatus(bools.forall(identity))
    } time { case(status, duration) =>  LTSVLogger.info("MemcachedStatus" -> status, "Time taken" -> toRelevantUnit(duration)) }
  }

  /**
   * Check whether there are any Hystrix commands whose circuit breakers are currently open.
   */
  private def checkHystrix(): HystrixStatus = {
    val startTime = System.nanoTime()
    val commandKeysWithOpenCircuitBreakers = hystrixHealthReporter.getCommandKeysWithOpenCircuitBreakers
    val ok = commandKeysWithOpenCircuitBreakers.isEmpty
    val hystrixStatus = HystrixStatus(ok, commandKeysWithOpenCircuitBreakers)
    LTSVLogger.info("HystrixStatus" -> hystrixStatus, "Time taken" -> toRelevantUnit(Duration(System.nanoTime() - startTime, TimeUnit.NANOSECONDS)))
    hystrixStatus
  }
}

object HealthcheckController {

  sealed trait ServiceStatus {
    def ok: Boolean
  }

  case class DbStatus(ok: Boolean, message: String) extends ServiceStatus

  case class HystrixStatus(ok: Boolean, openCircuits: Seq[String]) extends ServiceStatus

  case class MemcachedStatus(ok: Boolean) extends ServiceStatus

  object ServiceHealth {
    val Green = "GREEN"
    val Yellow = "YELLOW"
  }

  case class ServiceHealth(colour: String, statuses: Map[String, ServiceStatus]) {
    def healthy: Boolean = colour == ServiceHealth.Green
  }

  implicit val dbStatusWrites = Json.writes[DbStatus]
  implicit val hystrixStatusWrites = Json.writes[HystrixStatus]
  implicit val memcachedStatusWrites = Json.writes[MemcachedStatus]
  implicit val statusWrites = new Writes[ServiceStatus] {
    def writes(status: ServiceStatus) = status match {
      case db: DbStatus => implicitly[Writes[DbStatus]].writes(db)
      case hystrix: HystrixStatus => implicitly[Writes[HystrixStatus]].writes(hystrix)
      case memcached: MemcachedStatus => implicitly[Writes[MemcachedStatus]].writes(memcached)
    }
  }
  implicit val serviceHealthWrites = Json.writes[ServiceHealth]

  private def logIfUnhealthy(health: ServiceHealth): Unit = {
    if (!health.healthy) {
      LTSVLogger.warn("health" -> "unhealthy", "colour" -> health.colour, "statuses" -> health.statuses.toString)
    }
  }

  private def calculateColour(statuses: Iterable[ServiceStatus]): String = {
    if (statuses.forall(_.ok)) {
      ServiceHealth.Green
    } else {
      ServiceHealth.Yellow
    }
  }
}

