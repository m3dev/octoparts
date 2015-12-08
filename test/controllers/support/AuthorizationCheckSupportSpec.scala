package controllers.support

import com.m3.octoparts.auth.{ OctopartsAuthHandler, PrincipalSessionPersistence }
import com.m3.octoparts.support.PlayAppSupport
import org.scalatest.{ FlatSpec, Matchers }
import play.api.mvc.Action
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future

class AuthorizationCheckSupportSpec extends FlatSpec with Matchers with PlayAppSupport
    with AuthenticationCheckSupport with AuthorizationCheckSupport {

  val authHandler: Option[OctopartsAuthHandler] = None

  val acceptedRoles = Set("king", "duke")

  val action = Action.
    andThen(authenticationCheckFilter(_ => Future.successful(Unauthorized))).
    andThen(authorizationCheckFilter(
      req => Future.successful(req.principal.roles.toSet.intersect(acceptedRoles).nonEmpty),
      _ => Future.successful(Forbidden))).
    apply(req => Ok(s"Hi, ${req.principal.nickname}"))

  it should "forbid an unauthorized user" in {
    val result = action.apply(FakeRequest().
      withSession(PrincipalSessionPersistence.SessionKey -> """{"id": "chris", "nickname": "Chris", "email": "c-birchall@m3.com", "roles": ["serf", "peon"]}"""))
    status(result) should be(403)
  }

  it should "render the page for a user with an accepted role" in {
    val result = action.apply(FakeRequest().
      withSession(PrincipalSessionPersistence.SessionKey -> """{"id": "chris", "nickname": "Chris", "email": "c-birchall@m3.com", "roles": ["king", "emperor"]}"""))
    status(result) should be(200)
    contentAsString(result) should be("Hi, Chris")
  }

}
