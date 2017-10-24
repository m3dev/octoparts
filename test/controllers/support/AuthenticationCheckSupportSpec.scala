package controllers.support

import com.m3.octoparts.auth.{ PrincipalSessionPersistence, OctopartsAuthHandler }
import com.m3.octoparts.support.PlayAppSupport
import org.scalatest.{ FlatSpec, Matchers }
import play.api.mvc.Action
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future

class AuthenticationCheckSupportSpec extends FlatSpec with Matchers with PlayAppSupport with AuthenticationCheckSupport {

  val authHandler: Option[OctopartsAuthHandler] = None

  val action = appComponents.controllerComponents.actionBuilder.andThen(authenticationCheckFilter(_ => Future.successful(Redirect("/login")))).apply(req => Ok(s"Hi, ${req.principal.nickname}"))

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
