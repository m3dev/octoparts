package controllers

import javax.ws.rs.PathParam

import com.m3.octoparts.cache.client.CacheClient
import com.m3.octoparts.cache.versioning.VersionedParamKey
import com.m3.octoparts.model.config.CacheGroup
import com.m3.octoparts.repository.ConfigsRepository
import com.wordnik.swagger.annotations._
import controllers.support.LoggingSupport
import play.api.mvc.{ Action, Controller, RequestHeader, Result }

import scala.concurrent.Future
import scala.util.control.NonFatal

@Api(
  value = "/cache/invalidate",
  description = "Octoparts' cache invalidation API",
  produces = "text/plain",
  consumes = "application/json"
)
class CacheController(cacheClient: CacheClient, repository: ConfigsRepository)
    extends Controller
    with LoggingSupport {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  @ApiOperation(
    value = "Invalidate the cache for a specific endpoint",
    nickname = "Wipe out cache for an endpoint",
    notes = "Looks up a registered endpoint by id and clears the cache for it.",
    response = classOf[String],
    httpMethod = "POST"
  )
  def invalidatePart(
    @ApiParam(value = "The id of the endpoint that you you wish to invalidate", required = true)@PathParam("partId") partId: String) = Action.async { implicit request =>
    // TODO could check if part exists and return a 404 if not
    debugRc("action" -> "invalidateAll", "partId" -> partId)
    checkResult(cacheClient.increasePartVersion(partId))
  }

  @ApiOperation(
    value = "Invalidate a portion of cache for a specific endpoints",
    nickname = "Wipe out some of the cache for an endpoint",
    notes = "Looks up a registered endpoint by id and clears the portion of cache corresponding to a given parameter name and parameter value. Useful for example if you have 'user_id' registered as a paremeter for a backend endpoint and need to clear cache for a specific user id.",
    response = classOf[String],
    httpMethod = "POST"
  )
  def invalidatePartParam(
    @ApiParam(value = "The id of the endpoint that you you wish to invalidate", required = true)@PathParam("partId") partId: String,
    @ApiParam(value = "The parameter name that you wish to invalidate with", required = true)@PathParam("paramName") paramName: String,
    @ApiParam(value = "The specific parameter value that you wish to invalidate by", required = true)@PathParam("paramValue") paramValue: String) = Action.async { implicit request =>
    // TODO could check if part exists and return a 404 if not
    debugRc("action" -> "invalidateAll", "partId" -> partId, "pname" -> paramName, "pvalue" -> paramValue)
    checkResult(cacheClient.increaseParamVersion(VersionedParamKey(partId, paramName, paramValue)))
  }

  @ApiOperation(
    value = "Invalidate the cache of the endpoints registered to a cache group",
    nickname = "Wipe out cache for CacheGroup endpoints",
    notes = "Given the name of a CacheGroup, looks up the endpoints registered to it and wipes out all their caches.",
    response = classOf[String],
    httpMethod = "POST"
  )
  @ApiResponses(Array(new ApiResponse(code = 404, message = "Cache group not found")))
  def invalidateCacheGroupParts(
    @ApiParam(value = "The name of the CacheGroup that you wish to invalidate", required = true)@PathParam("cacheGroupName") cacheGroupName: String) = Action.async { implicit request =>
    val fMaybeCacheGroup = repository.findCacheGroupByName(cacheGroupName)
    fMaybeCacheGroup.flatMap[Result] { maybeCacheGroup =>
      maybeCacheGroup.fold {
        Future.successful(NotFound(s"CacheGroup $cacheGroupName not found"))
      } { group =>
        debugRc("action" -> "invalidateCacheGroupParts", "group" -> group.name)
        invalidateGroupParts(group).map(renderInvalidated).recover(logAndRenderError("ERROR: " + _.toString))
      }
    }
  }

  @ApiOperation(
    value = "Invalidate parameter-value-specific cache of the endpoints registered to a cache group",
    nickname = "Wipe out paramValue-specific cache for CacheGroup endpoints",
    notes = "Given the name of a CacheGroup, looks up the endpoints registered to it and wipes out the portion of their caches that correspond to the given parameter value. This works because for a given endpoint, you can choose to register specific parameters to a cache group.",
    response = classOf[String],
    httpMethod = "POST"
  )
  @ApiResponses(Array(new ApiResponse(code = 404, message = "Cache group not found")))
  def invalidateCacheGroupParam(
    @ApiParam(value = "The name of the CacheGroup that you wish to invalidate", required = true)@PathParam("cacheGroupName") cacheGroupName: String,
    @ApiParam(value = "The specific parameter value that you wish to invalidate by", required = true)@PathParam("paramValue") paramValue: String) = Action.async { implicit request =>
    val fMaybeCacheGroup = repository.findCacheGroupByName(cacheGroupName)
    fMaybeCacheGroup.flatMap { maybeCacheGroup =>
      maybeCacheGroup.fold {
        Future.successful(NotFound(s"CacheGroup $cacheGroupName not found"))
      } { group =>
        debugRc("action" -> "invalidateCacheGroupParams", "group" -> group.name, "pvalue" -> paramValue)
        invalidateGroupPartParams(group, paramValue).map(renderInvalidated).recover(logAndRenderError("ERROR: " + _.toString))
      }
    }
  }

  private def checkResult(fu: Future[Unit])(implicit request: RequestHeader): Future[Result] = {
    fu.map(_ => Ok("OK")).recover(logAndRenderError("ERROR: " + _.toString))
  }

  private def renderInvalidated[A](invalidatedThings: Seq[A]) = Ok(s"OK: invalidated the following: $invalidatedThings")

  private def logAndRenderError(render: Throwable => String)(implicit request: RequestHeader): PartialFunction[Throwable, Result] = {
    case NonFatal(err) =>
      errorRc(err)
      InternalServerError(render(err))
  }

  /**
   * Invalidates the given group's PartParams
   *
   * Does a fetch of each PartParam that is on the CacheGroup because
   * 1. These may be stale because CacheGroups are cached (unlikely)
   * 2. We CANNOT assume that PartParams from CacheGroup#partParams have their parent HttpPartConfig
   * readily available
   *
   * @return Future sequence of partParam names that were invalidated
   */
  private def invalidateGroupPartParams(group: CacheGroup, paramValue: String): Future[Seq[String]] = {
    val futureMaybeParamSeq = Future.sequence(group.partParams.map(_.id).flatten.map(repository.findParamById))
    futureMaybeParamSeq.flatMap { maybeParamSeq =>
      Future.sequence(for {
        maybeParam <- maybeParamSeq
        param <- maybeParam
        partConfig <- param.httpPartConfig // Safe to assume at this point that they exist
      } yield {
        cacheClient.increaseParamVersion(VersionedParamKey(partConfig.partId, param.outputName, paramValue)).map { done =>
          param.outputName
        }
      }
      )
    }
  }

  /**
   * Invalidates the given group's parts
   * @return Future sequence of Part names that were invalidated
   */
  private def invalidateGroupParts(group: CacheGroup): Future[Seq[String]] = Future.sequence(for {
    part <- group.httpPartConfigs
  } yield {
    cacheClient.increasePartVersion(part.partId).map(done => part.partId)
  })

}
