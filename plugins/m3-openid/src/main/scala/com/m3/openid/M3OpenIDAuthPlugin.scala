package com.m3.openid

import java.io.IOException

import com.m3.octoparts.auth.{ Principal, AuthenticatedRequest, OctopartsAuthPlugin }
import play.api.libs.openid.{ UserInfo, OpenID }
import play.api.mvc.{ Request, Results }
import play.api.{ Application, Logger }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * An implementation of [[com.m3.octoparts.auth.OctopartsAuthPlugin]] that performs
 * authentication/authorization using the M3 internal OpenID provider.
 *
 * A user is judged to be authenticated if they have one of the accepted roles.
 */
class M3OpenIDAuthPlugin(application: Application) extends OctopartsAuthPlugin with Results {

  private val openidProviderUrl = application.configuration.underlying.getString("auth.openid.providerUrl")

  private val requiredAxAttributes = Seq(
    "nickname" -> "http://m3.com/nickname",
    "email" -> "http://m3.com/email",
    "groups" -> "http://m3.com/groups"
  )

  private val acceptedRoles: Set[String] = application.configuration.getStringSeq("auth.acceptedRoles").getOrElse(Nil).toSet

  /**
   * Add the line "auth.disabled = true" if you want to disable the plugin.
   */
  override def enabled = !isDisabled

  private def isDisabled = application.configuration.getBoolean("auth.disabled").getOrElse(false)

  /**
   * What action to take when a request is not authenticated, e.g. redirect to an authentication server.
   *
   * @param request The unauthenticated request
   * @param callbackUrl An absolute URL that you can use for a post-authentication callback
   */
  def onNotAuthenticated(request: Request[_], callbackUrl: String)(implicit exec: ExecutionContext) = {
    val redirectUrl = OpenID.redirectURL(
      openID = openidProviderUrl,
      callbackURL = callbackUrl,
      axRequired = requiredAxAttributes.toSeq)
    redirectUrl.map(Redirect(_))
  }

  /**
   * Whether the given principal is authorized to use the Octoparts admin UI.
   *
   * @param authenticatedRequest an authenticated request with a [[com.m3.octoparts.auth.Principal]]
   * @return your decision
   */
  def isAuthorized(authenticatedRequest: AuthenticatedRequest[_])(implicit exec: ExecutionContext) = {
    val validRoles = authenticatedRequest.principal.roles.toSet.intersect(acceptedRoles)
    if (validRoles.isEmpty) {
      // User does not have a valid role
      Future.successful(false)
    } else {
      Logger.debug(s"Accepting user [${authenticatedRequest.principal.nickname}] because they have these roles: $validRoles")
      Future.successful(true)
    }
  }

  /**
   * What action to take when a request is authenticated but the principal is not authorized.
   *
   * @param authenticatedRequest an authenticated request with a [[com.m3.octoparts.auth.Principal]]
   */
  def onUnauthorized(authenticatedRequest: AuthenticatedRequest[_])(implicit exec: ExecutionContext) = {
    Future.successful(Forbidden)
  }

  /**
   * Use a post-authentication callback from an external authentication provider to construct a [[com.m3.octoparts.auth.Principal]]
   *
   * @return a Principal constructed from the HTTP request
   */
  def onAuthenticationCallback(request: Request[_])(implicit exec: ExecutionContext) = {
    OpenID.verifiedId(request).map { info =>
      userInfo2Principal(info).fold {
        // Failed to extract a principal object from the info returned by the OpenID provider. Missing attribute?
        throw new IOException(s"OpenID authentication failed. Most likely your OpenID provider did not return all the attributes we need. Here's what we got: $info")
      } { principal =>
        Logger.debug(s"OpenID auth completed for ${info.id}.")
        principal
      }
    }
  }

  private def userInfo2Principal(info: UserInfo): Option[Principal] = {
    val principal = for {
      nickname <- info.attributes.get("nickname")
      email <- info.attributes.get("email")
      roles <- info.attributes.get("groups")
    } yield Principal(info.id, nickname, email, roles.split(", ?"))

    if (!principal.isDefined) {
      Logger.warn("Failed to build a Principal from the info returned by the OpenID provider. " +
        s"Attributes returned: ${info.attributes}. " +
        "We require the following attributes: nickname, email, roles")
    }

    principal
  }

}
