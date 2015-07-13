package integration

import com.m3.octoparts.support.PlayAppSupport
import org.scalatest.{ FunSpec, Matchers }
import play.api.test.FakeRequest
import play.api.test.Helpers._

class ApplicationSpec extends FunSpec with PlayAppSupport with Matchers {

  describe("/") {

    it("should return OK") {
      val result = route(FakeRequest(GET, "/")).get
      contentAsString(result) should include("Parts</title>")
    }

  }

}
