package controllers.support

import com.beachape.logging.{ LTSVLoggerLike, LTSVLogger }
import play.api.Logger
import play.api.mvc.RequestHeader

trait LoggingSupport extends LTSVLoggerLike {

  val underlying = Logger.underlyingLogger

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
  @inline protected def warnRc(msg: (String, String)*)(implicit request: RequestHeader) = warn(buildRequestContext ++ msg: _*)
  @inline protected def warnRc(msg: (String, String), e: Throwable)(implicit request: RequestHeader) = warn(e, (buildRequestContext :+ msg): _*)
  @inline protected def errorRc(e: Throwable)(implicit request: RequestHeader) = error(e, buildRequestContext: _*)
  @inline protected def errorRc(msg: (String, String), e: Throwable)(implicit request: RequestHeader) = error(e, (buildRequestContext :+ msg): _*)
  @inline protected def debugRc(implicit request: RequestHeader) = debug(buildRequestContext: _*)
  @inline protected def debugRc(msg: (String, String)*)(implicit request: RequestHeader) = debug(buildRequestContext ++ msg: _*)
  @inline protected def infoRc(implicit request: RequestHeader) = info(buildRequestContext: _*)
  @inline protected def infoRc(msg: (String, String)*)(implicit request: RequestHeader) = info(buildRequestContext ++ msg: _*)

}