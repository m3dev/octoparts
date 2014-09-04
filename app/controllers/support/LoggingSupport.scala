package controllers.support

import play.api.Logger
import play.api.mvc.RequestHeader
import skinny.util.LTSV

object LoggingSupport {

  def toLtsv(items: Seq[(String, String)]): String = {
    val contextItems: Seq[(String, String)] = items.filterNot { case (k, v) => v == null || v.isEmpty }
    LTSV.dump(contextItems: _*)
  }
}

trait LoggingSupport {
  import controllers.support.LoggingSupport._

  /**
   * @return remote address, uri, query
   */
  protected def buildRequestContext(implicit request: RequestHeader): Seq[(String, String)] = Seq(
    "From" -> request.remoteAddress,
    "Uri" -> request.uri,
    "Query" -> request.rawQueryString,
    "Referer" -> request.headers.get("Referer").getOrElse("")
  )

  // those shortcuts make sure to print the requestContext
  @inline protected def warnRc(msg: (String, String)*)(implicit request: RequestHeader) = Logger.warn(toLtsv(buildRequestContext ++ msg))
  @inline protected def warnRc(msg: (String, String), e: Throwable)(implicit request: RequestHeader) = Logger.warn(toLtsv(buildRequestContext ++ Seq(msg, "Error" -> e.toString)), e)
  @inline protected def errorRc(e: Throwable)(implicit request: RequestHeader) = Logger.error(toLtsv(buildRequestContext), e)
  @inline protected def errorRc(msg: (String, String), e: Throwable)(implicit request: RequestHeader) = Logger.error(toLtsv(buildRequestContext ++ Seq(msg, "Error" -> e.toString)), e)
  @inline protected def debugRc(implicit request: RequestHeader) = Logger.debug(toLtsv(buildRequestContext))
  @inline protected def debugRc(msg: (String, String)*)(implicit request: RequestHeader) = Logger.debug(toLtsv(buildRequestContext ++ msg))
  @inline protected def infoRc(implicit request: RequestHeader) = Logger.info(toLtsv(buildRequestContext))
  @inline protected def infoRc(msg: (String, String)*)(implicit request: RequestHeader) = Logger.info(toLtsv(buildRequestContext ++ msg))

}