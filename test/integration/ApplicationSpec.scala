package integration

import com.m3.octoparts.support.PlayAppSupport
import org.scalatest.{ FunSpec, Matchers }
import play.api.test.FakeRequest
import play.api.test.Helpers._

class ApplicationSpec extends FunSpec with PlayAppSupport with Matchers {

  def shouldReturnOk(path: String, shouldContain: Option[String] = None): Unit = {
    it(s"should return $OK for $path") {
      val result = route(app, FakeRequest(GET, path)).get
      status(result) shouldBe OK
      shouldContain.foreach { s =>
        contentAsString(result) should include(s)
      }
    }
  }

  shouldReturnOk("/", Some("Parts</title>"))

  shouldReturnOk("/hystrix.stream")

  describe("system info paths") {

    def systemPath(subPath: String) = s"/system/$subPath"

    shouldReturnOk(systemPath("build"))
    shouldReturnOk(systemPath("config"))
    shouldReturnOk(systemPath("config/logger"))
    shouldReturnOk(systemPath("healthcheck"))
    shouldReturnOk(systemPath("metrics"))
  }

}