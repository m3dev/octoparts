package integration

import com.m3.octoparts.model.config.json.ThreadPoolConfig
import com.m3.octoparts.support.db.RequiresDB
import com.m3.octoparts.support.{ PlayServerSupport, OneDIBrowserPerSuite }
import org.openqa.selenium.support.ui.Select
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

  object PartAddPage extends SeleniumPage {
    val url: String = s"$baseUrl/admin/parts/new"
  }

  def PartEditPage(partId: String): SeleniumPage = new SeleniumPage {
    val url: String = s"$baseUrl/admin/parts/$partId/edit"
  }

  def uniqueId = java.util.UUID.randomUUID

  def createThreadPool: ThreadPoolConfig = {
    val name = s"my little pool $uniqueId"
    val size = 10
    val queueSize = 500
    goTo(ThreadPoolAddPage)
    textField("threadPoolKey").value = name
    numberField("coreSize").value = s"$size"
    numberField("queueSize").value = s"$queueSize"
    submit()
    ThreadPoolConfig(name, size, queueSize)
  }

  private def createPart(threadPoolConfig: ThreadPoolConfig): TestPartConfig = {
    val ThreadPoolConfig(threadPoolName, coreSize, _) = threadPoolConfig
    goTo(PartAddPage)
    val partId = s"my little part $uniqueId"
    val url = "http://beachape.com"
    val connectionPoolSize = 5
    val commandKey = s"part_key_$uniqueId"
    val commandKeyGroup = s"part_group_$uniqueId"
    val proxy = "http://proxy.com"
    textField("partId").value = partId
    urlField("httpSettings.uri").value = url
    numberField("httpSettings.httpPoolSize").value = connectionPoolSize.toString
    textField("hystrixConfig.commandKey").value = commandKey
    textField("hystrixConfig.commandGroupKey").value = commandKeyGroup
    textField("httpSettings.httpProxy").value = proxy
    val threadPoolDropdown = new Select(webDriver.findElement(IdQuery("hystrixConfig_threadPoolConfigId").by))
    threadPoolDropdown.selectByVisibleText(s"$threadPoolName (core size: $coreSize)")
    submit()
    TestPartConfig(
      partId = partId,
      url = url,
      connectionPoolSize = connectionPoolSize,
      commandKey = commandKey,
      commandKeyGroup = commandKeyGroup,
      proxy = proxy
    )
  }

  describe("adding a thread pool") {

    it("should work and redirect me to the show page") {
      val ThreadPoolConfig(name, size, queueSize) = createThreadPool
      pageTitle should include("Thread pool details")
      val descriptors = findAll(TagNameQuery("dd")).toSeq
      descriptors.find(_.text == name) shouldBe 'defined
      descriptors.find(_.text == size.toString) shouldBe 'defined
      descriptors.find(_.text == queueSize.toString) shouldBe 'defined
    }

  }

  describe("Part administration") {

    describe("adding a Part") {

      it("should work and redirect me to the part show page") {
        val TestPartConfig(partId, url, connectionPoolSize, commandKey, commandKeyGroup, proxy) = createPart(createThreadPool)
        pageTitle should include("Part details")
        val descriptors = findAll(TagNameQuery("dd")).toSeq
        Seq(partId, url, connectionPoolSize, commandKey, commandKeyGroup, proxy).foreach { v =>
          descriptors.find(_.text.trim == v.toString) shouldBe 'defined
        }
      }

    }

    describe("editing a part") {

      it("should result in a redirect to the main Parts listing page if there is no such partId") {
        goTo(PartEditPage("lalala-wha-wha-whaaat"))
        currentUrl should not endWith "/edit"
        pageTitle should include("Parts")
      }

      it("should send me to the part show page after successful editing") {
        val TestPartConfig(partId, _, _, _, _, _) = createPart(createThreadPool)
        goTo(PartEditPage(partId))
        currentUrl should endWith("/edit")
        val newUrl = "http://new-hotness.com"
        urlField("httpSettings.uri").value = newUrl
        submit()
        currentUrl should endWith("/show")
        val descriptors = findAll(TagNameQuery("dd")).toSeq
        descriptors.find(_.text == newUrl) shouldBe 'defined
      }
    }

  }

  private case class TestPartConfig(partId: String,
                                    url: String,
                                    connectionPoolSize: Int,
                                    commandKey: String,
                                    commandKeyGroup: String,
                                    proxy: String)
}