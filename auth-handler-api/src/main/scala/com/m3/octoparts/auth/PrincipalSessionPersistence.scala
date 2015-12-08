package com.m3.octoparts.auth

import com.beachape.logging.LTSVLogger
import play.api.libs.json.{ JsSuccess, JsError, Json }
import play.api.mvc.{ Request, Result, Session }

import scala.util.{ Success, Failure, Try }

/**
 * Helper methods for persisting the Principal using the Play session cookie.
 * This is the default behaviour of the auth plugin, but custom auth plugins may want to override it,
 * e.g. to store sessions in a DB.
 */
object PrincipalSessionPersistence {

  /** The key used to store the principal in the Play session */
  val SessionKey = "principal"

  /**
   * Look up the Principal object in the Play session and deserialize it from Json
   */
  def extractPrincipalFromPlaySession(session: Session): Option[Principal] = {
    session.get(SessionKey).flatMap { sessionValue =>
      Try(Json.parse(sessionValue)) match {
        case Failure(e) =>
          LTSVLogger.warn("Rejecting invalid principal found in session (not valid json)" -> sessionValue)
          None
        case Success(json) =>
          json.validate[Principal] match {
            case JsError(_) =>
              LTSVLogger.warn("Rejecting invalid principal found in session (unexpected json)" -> sessionValue)
              None
            case JsSuccess(principal, _) =>
              // Everything's OK
              Some(principal)
          }
      }
    }
  }

  /**
   * Persist the Principal in the Play session cookie
   */
  def savePrincipalToPlaySession(request: Request[_], result: Result, principal: Principal): Result =
    result.addingToSession(SessionKey -> Json.toJson(principal).toString)(request)

  /**
   * Delete the Principal object from the Play session cookie
   */
  def deletePrincipalFromPlaySession(request: Request[_], result: Result): Result = result.removingFromSession(SessionKey)(request)

}
