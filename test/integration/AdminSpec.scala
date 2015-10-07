package integration

import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.scalatest.{ Matchers, FunSpec }
import org.scalatest.selenium.{ Page => SeleniumPage }
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatestplus.play.{ HtmlUnitFactory, OneBrowserPerSuite, OneServerPerSuite }

class AdminSpec
    extends FunSpec
    with OneServerPerSuite
    with OneBrowserPerSuite
    with HtmlUnitFactory
    with Matchers
    with ScalaFutures
    with IntegrationPatience {

  val htmlunitDriver = webDriver.asInstanceOf[HtmlUnitDriver]
  htmlunitDriver.setJavascriptEnabled(false)

  def baseUrl: String = {
    s"http://localhost:$port"
  }

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
      eventually {
        pageTitle should include("Thread pool details")
      }
      val descriptors = findAll(TagNameQuery("dd"))
      descriptors.find(_.text == name) shouldBe 'defined
      descriptors.find(_.text == size.toString) shouldBe 'defined
      descriptors.find(_.text == queueSize.toString) shouldBe 'defined
    }

  }
}
