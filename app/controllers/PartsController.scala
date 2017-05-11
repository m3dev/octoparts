package controllers

import javax.ws.rs.QueryParam

import akka.actor.ActorSystem
import com.beachape.zipkin.ReqHeaderToSpanImplicit
import com.beachape.zipkin.services.ZipkinServiceLike
import com.m3.octoparts.future.PromiseSupport
import com.m3.octoparts.json.format.ConfigModel._
import com.m3.octoparts.json.format.ReqResp._
import com.m3.octoparts.aggregator.service.PartsService
import com.m3.octoparts.model._
import com.m3.octoparts.model.config.HttpPartConfig
import com.m3.octoparts.repository.ConfigsRepository
import com.wordnik.swagger.annotations._
import controllers.support.{ LoggingSupport, PartListFilterSupport }
import org.apache.http.client.cache.HeaderConstants
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._

@Api(
  value = "/octoparts/2",
  description = "Octoparts' backend endpoints API",
  produces = "application/json",
  consumes = "application/json"
)
class PartsController(
  partsService: PartsService,
  configsRepository: ConfigsRepository,
  requestTimeout: Duration,
  readClientCacheHeaders: Boolean,
  val actorSystem: ActorSystem,
  implicit val zipkinService: ZipkinServiceLike,
  controllerComponents: ControllerComponents
)(implicit eCtx: ExecutionContext)
    extends AbstractController(controllerComponents)
    with LoggingSupport
    with PartListFilterSupport
    with ReqHeaderToSpanImplicit
    with PromiseSupport {

  import com.beachape.zipkin.FutureEnrichment._

  @ApiOperation(
    value = "Invoke registered endpoints",
    nickname = "Endpoints invocation",
    notes =
    "Send an AggregateRequest to invoke backend endpoints. Will respond with an AggregateResponse for you to sort through.",
    response = classOf[AggregateResponse],
    httpMethod = "POST"
  )
  @ApiResponses(Array(new ApiResponse(code = 400, message = "Invalid input")))
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        value =
        "An AggregateRequest consisting of PartRequests that individually invoke a registered backend service once.",
        required = true,
        dataType = "com.m3.octoparts.model.AggregateRequest",
        paramType = "body",
        name = "body"
      )
    )
  )
  def retrieveParts = Action.async(parse.json) { implicit request =>
    request.body
      .validate[AggregateRequest]
      .fold[Future[Result]](
        errors => {
          warnRc(
            "Body" -> request.body.toString,
            "Errors" -> errors.toString
          )
          Future.successful(BadRequest("Unrecognized request object"))
        },
        aggregateRequest => {
          val noCache = readClientCacheHeaders &&
            request.headers
            .get(HeaderConstants.CACHE_CONTROL)
            .contains(HeaderConstants.CACHE_CONTROL_NO_CACHE)
          logAggregateRequest(aggregateRequest, noCache)
          val fAggregateResponse = partsService
            .processParts(aggregateRequest, noCache)
            .trace("aggregate-response-processing")
          withRequestTimeout(fAggregateResponse)
            .trace("aggregate-response-processing-with-timeout")
        }
      )
  }

  @ApiOperation(
    value = "Return a list of all registered endpoints in the system",
    nickname = "Endpoints listing",
    notes = "Returns a list of registered endpoints in the system.",
    response = classOf[HttpPartConfig],
    responseContainer = "List",
    httpMethod = "GET"
  )
  def list(
    @ApiParam(
      value =
      "Optional part ids to filter on. Note, this should be passed as multiple partIdParams=partId, e.g ?partIdParams=wut&partIdParams=wut3 ",
      allowMultiple = true
    )@QueryParam("partIdParams") partIdParams: List[String] = Nil
  ) =
    Action.async { implicit req =>
      retrieveParts(partIdParams)
    }

  @ApiOperation(
    value = "Return a list of all registered endpoints in the system, but sent via a POST",
    nickname = "Endpoints listing POST",
    notes =
    "Returns a list of registered endpoints in the system. Use this if you want to do filtering with so many IDs that you hit the URL limit of our server",
    response = classOf[HttpPartConfig],
    responseContainer = "List",
    httpMethod = "POST"
  )
  @ApiImplicitParams(
    Array(
      new ApiImplicitParam(
        value = "An array of ids",
        required = true,
        dataType = "controllers.support.PartListFilter",
        paramType = "body",
        name = "body"
      )
    )
  )
  def listPost = Action.async { implicit req =>
    partListFilterForm
      .bindFromRequest()
      .fold(
        hasErrors => Future.successful(BadRequest("Could no bind ids from request")),
        bound => retrieveParts(bound.ids)
      )
  }

  private def logAggregateRequest(
    aggregateRequest: AggregateRequest,
    noCache: Boolean
  )(implicit request: RequestHeader): Unit = {
    val logData = Seq(
      "requestId" -> aggregateRequest.requestMeta.id,
      "noCache" -> noCache.toString,
      "timeoutMs" -> aggregateRequest.requestMeta.timeout.fold("default")(_.toMillis.toString),
      "requestUrl" -> aggregateRequest.requestMeta.requestUrl.getOrElse("unknown"),
      "numParts" -> aggregateRequest.requests.size.toString
    )
    if (underlyingLogger.isDebugEnabled) debugRc(logData: _*) else info(logData: _*)
  }

  private def withRequestTimeout(fResponse: Future[AggregateResponse]): Future[Result] = {
    val fOkResponse = fResponse.map(aggResp => Ok(Json.toJson(aggResp)))
    val fTimeout = timeout(InternalServerError("Request timed out"), requestTimeout)
    Future.firstCompletedOf(Seq(fOkResponse, fTimeout))
  }

  private def retrieveParts(ids: Seq[String])(implicit req: Request[AnyContent]): Future[Result] = {
    debugRc
    val fConfigs = ids match {
      case Nil => configsRepository.findAllConfigs().trace("find-all-configs")
      case partIds => {
        val fParts = partIds.map { partId =>
          configsRepository
            .findConfigByPartId(partId)
            .trace(
              "find-config-by-part-ids",
              "ids" -> partId
            )
        }
        Future.sequence(fParts).map(_.flatten)
      }
    }
    for {
      configs <- fConfigs
    } yield {
      Ok(Json.toJson(configs.map(HttpPartConfig.toJsonModel)))
        .withHeaders("Content-disposition" -> "attachment; filename=exported-parts.json")
    }
  }

}
