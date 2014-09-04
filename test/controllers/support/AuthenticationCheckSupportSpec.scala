package controllers.support

import com.m3.octoparts.auth.{ PrincipalSessionPersistence, AuthenticatedRequest, OctopartsAuthPlugin, Principal }
import org.scalatest.{ FlatSpec, Matchers }
import org.scalatestplus.play.OneAppPerSuite
import play.api.mvc.{ Request, Action }
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.{ ExecutionContext, Future }

class AuthenticationCheckSupportSpec extends FlatSpec with Matchers with OneAppPerSuite with AuthenticationCheckSupport {
  import scala.concurrent.ExecutionContext.Implicits.global

  val authPlugin: Option[OctopartsAuthPlugin] = None

  val action = Action.andThen(authenticationCheckFilter(_ => Future.successful(Redirect("/login")))).apply(req => Ok(s"Hi, ${req.principal.nickname}"))

  it should "redirect an unauthenticated user to a login page" in {
    redirectLocation(action.apply(FakeRequest())) should be(Some("/login"))
  }

  it should "redirect a user with an invalid principal in their session to a login page" in {
    redirectLocation(action.apply(FakeRequest().withSession(PrincipalSessionPersistence.SessionKey -> "howdy!"))) should be(Some("/login"))
    redirectLocation(action.apply(FakeRequest().withSession(PrincipalSessionPersistence.SessionKey -> """{"hi": "there"}"""))) should be(Some("/login"))
  }

  it should "render the page for a user with a valid principal in their session" in {
    val result = action.apply(FakeRequest().withSession(PrincipalSessionPersistence.SessionKey -> """{"id": "chris", "nickname": "Chris", "email": "c-birchall@m3.com", "roles": ["Engineering"]}"""))
    status(result) should be(200)
    contentAsString(result) should be("Hi, Chris")
  }

}
