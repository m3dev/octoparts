package controllers.support

import com.m3.octoparts.auth.{ PrincipalSessionPersistence, Principal }
import org.scalatest.{ FlatSpec, Matchers }
import org.scalatestplus.play.OneAppPerSuite
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers._

class DummyPrincipalSupportSpec extends FlatSpec with Matchers with OneAppPerSuite with DummyPrincipalSupport {

  val action = Action.andThen(autoAuthenticateRequest).apply(req => Ok(s"Hi, ${req.principal.nickname}"))

  it should "automatically create a guest principal and add it to the session" in {
    val result = action.apply(FakeRequest())
    contentAsString(result) should be("Hi, guest")
    val sessionValue = session(result).get(PrincipalSessionPersistence.SessionKey).get
    val principal = Json.parse(sessionValue).as[Principal]

    principal.id should startWith("guest")
    principal.nickname should be("guest")
    principal.email should startWith("guest@")
    principal.roles should be(Nil)
  }

  it should "render the page normally for a user with a valid principal in their session" in {
    val result = action.apply(FakeRequest().withSession(PrincipalSessionPersistence.SessionKey -> """{"id": "chris", "nickname": "Chris", "email": "c-birchall@m3.com", "roles": ["Engineering"]}"""))
    status(result) should be(200)
    contentAsString(result) should be("Hi, Chris")
    session(result).get(PrincipalSessionPersistence.SessionKey) should be(None) // because session hasn't changed, Play doesn't set the cookie
  }

}
