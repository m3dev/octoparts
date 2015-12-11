package integration

import com.m3.octoparts.support.db.RequiresDB
import com.m3.octoparts.support.{ PlayServerSupport, OneDIBrowserPerSuite }
import org.scalatest.{ Matchers, FunSpec }
import org.scalatest.selenium.{ Page => SeleniumPage }
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatestplus.play._

class AdminSpec
    extends FunSpec
    with PlayServerSupport
    with OneDIBrowserPerSuite
    with RequiresDB
    with HtmlUnitFactory
    with Matchers
    with ScalaFutures
    with IntegrationPatience {

  lazy val baseUrl: String = s"http://localhost:$port"

  object ThreadPoolAddPage extends SeleniumPage {
    val url: String = s"$baseUrl/admin/thread-pools/new"
  }

  describe("adding a thread pool") {

    it("should work and redirect me to the show page") {
      val name = s"my little pool ${java.util.UUID.randomUUID}"
      val size = 10
      val queueSize = 500
      goTo(ThreadPoolAddPage)
      textField("threadPoolKey").value = name
      numberField("coreSize").value = s"$size"
      numberField("queueSize").value = s"$queueSize"
      submit()
      pageTitle should include("Thread pool details")
      val descriptors = findAll(TagNameQuery("dd"))
      descriptors.find(_.text == name) shouldBe 'defined
      descriptors.find(_.text == size.toString) shouldBe 'defined
      descriptors.find(_.text == queueSize.toString) shouldBe 'defined
    }

  }

}