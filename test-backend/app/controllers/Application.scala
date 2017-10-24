package controllers

import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Promise


object Application extends Controller {

  def fast = Action{
    Ok("Quick as a flash!")
  }

  def slow(delay: Int) = Action.async {
    Promise.timeout(Ok("Sorry for the wait!"), delay)
  }

  def flaky(successRate: Double) = Action {
    if (util.Random.nextDouble <= successRate)
      Ok("No problem!")
    else
      InternalServerError("Whoops!")
  }

  /**
   * Emulates a backend with ETag support and/or custom Cache-Control header.
   *
   * If passed an `etag` query param,
   * - if request has an `If-None-Match` header, and it matches the query param, returns a 304.
   * - otherwise, return a 200 with an `ETag` header attached.
   *
   * If passed a `cacheControl` query param, attaches it to the 200 response.
   */
  def cacheHeaders(etag: String, cacheControl: Option[String]) = Action { request =>
    if (request.headers.get(IF_NONE_MATCH) == Some(etag)) {
      NotModified
    } else {
      val result = Ok(s"ETag: $etag, cache-control: $cacheControl").withHeaders(ETAG -> etag)
      cacheControl.fold[Result](result)(cc => result.withHeaders(CACHE_CONTROL -> cc))
    }
  }

}
