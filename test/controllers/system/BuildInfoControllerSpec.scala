package controllers.system

import com.m3.octoparts.BuildInfo
import com.m3.octoparts.support.PlayAppSupport
import org.scalatest.{ FlatSpec, Matchers }
import play.api.test.FakeRequest
import play.api.test.Helpers._

class BuildInfoControllerSpec extends FlatSpec with Matchers with JsonCheckSupport with PlayAppSupport {

  val subject = new BuildInfoController(appComponents.controllerComponents)

  it should "return a 200 OK response" in {
    status(subject.showBuildInfo.apply(FakeRequest())) should be(200)
  }

  it should "be valid json" in {
    val result = subject.showBuildInfo.apply(FakeRequest())
    checkJson(result) { implicit json =>
      string("version") should be(BuildInfo.version)
      string("scala_version") should be(BuildInfo.scalaVersion)
      string("git_branch") should be(BuildInfo.gitBranch)
    }
  }

}
