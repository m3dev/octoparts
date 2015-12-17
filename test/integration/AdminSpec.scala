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

  }

  object PartAddPage extends SeleniumPage {

    case class PartConfig(partId: String,
                          url: String,
                          connectionPoolSize: Int,
                          commandKey: String,
                          commandKeyGroup: String,
                          proxy: String)

    val url: String = s"$baseUrl/admin/parts/new"

    def createPart(threadPoolConfig: ThreadPoolConfig): PartConfig = {
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
      PartConfig(
        partId = partId,
        url = url,
        connectionPoolSize = connectionPoolSize,
        commandKey = commandKey,
        commandKeyGroup = commandKeyGroup,
        proxy = proxy
      )
    }
  }

  case class PartEditPage(partId: String) extends SeleniumPage {
    val url: String = s"$baseUrl/admin/parts/$partId/edit"
  }

  object CacheGroupListPage extends SeleniumPage {
    val url: String = s"$baseUrl/admin/cache-groups"
  }

  object CacheGroupAddPage extends SeleniumPage {
    val url: String = s"$baseUrl/admin/cache-groups/new"

    /**
     * Adds a Cache Group and gives back the name
     */
    def createCacheGroup: String = {
      goTo(this)
      val name = s"my little cache group $uniqueId"
      textField("name").value = name
      textField("description").value = "whatevs"
      submit()
      name
    }
  }

  case class CacheGroupShowPage(cacheGroupName: String) extends SeleniumPage {
    val url: String = s"$baseUrl/admin/cache-groups/$cacheGroupName/show"
  }

  case class CacheGroupEditPage(cacheGroupName: String) extends SeleniumPage {
    val url: String = s"$baseUrl/admin/cache-groups/$cacheGroupName/edit"

    def updateWithName(name: String) = {
      goTo(this)
      textField("name").value = name
      submit()
    }
  }

  case class CacheGroupDeletePage(cacheGroupName: String) extends SeleniumPage {
    val url: String = s"$baseUrl/admin/cache-groups/$cacheGroupName/delete"

    def delete() = {
      find(TagNameQuery("input")).filter(_.attribute("value").contains("Delete")).foreach(clickOn)
    }

    def cancel() = click on linkText("Cancel")
  }

  def uniqueId = java.util.UUID.randomUUID

  describe("adding a thread pool") {

    it("should work and redirect me to the show page") {
      val ThreadPoolConfig(name, size, queueSize) = ThreadPoolAddPage.createThreadPool
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
        val PartAddPage.PartConfig(partId, url, connectionPoolSize, commandKey, commandKeyGroup, proxy) = PartAddPage.createPart(ThreadPoolAddPage.createThreadPool)
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
        val PartAddPage.PartConfig(partId, _, _, _, _, _) = PartAddPage.createPart(ThreadPoolAddPage.createThreadPool)
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

  describe("Cache groups administration") {

    describe("the listing page") {

      it("should work fine and display all the existing groups") {
        // create a few CacheGroups
        val groupNames = (1 to 5).map(_ => CacheGroupAddPage.createCacheGroup)
        goTo(CacheGroupListPage)
        pageTitle should include("Cache groups")
        val rows = findAll(TagNameQuery("td")).toSeq
        groupNames.foreach { groupName =>
          rows.find(_.text == groupName) shouldBe 'defined
        }
      }

      it("should have a button for creating a new cache group") {
        goTo(CacheGroupListPage)
        val addNewLink = find(LinkTextQuery("Create a new cache group")).get
        addNewLink.attribute("href") shouldBe Some(CacheGroupAddPage.url)
      }

    }

    describe("the add CacheGroup page") {

      it("should allow me to add a cache group") {
        val newCacheGroupName = CacheGroupAddPage.createCacheGroup
        goTo(CacheGroupListPage)
        pageSource should include(newCacheGroupName)
      }

    }

    describe("the show CacheGroup page") {

      it("should redirect me to the cache group list page if no such cache group can be found") {
        goTo(CacheGroupShowPage("boyakasha"))
        currentUrl shouldBe CacheGroupListPage.url
      }

      it("should show me a cache group if the cache group exists") {
        val name = CacheGroupAddPage.createCacheGroup
        goTo(CacheGroupShowPage(name))
        pageTitle should include("Cache group details")
        val descriptors = findAll(TagNameQuery("dd")).toSeq
        descriptors.find(_.text == name) shouldBe 'defined
      }

    }

    describe("the edit CacheGroup page") {

      it("should redirect me to the cache group list page if no such cache group can be found") {
        goTo(CacheGroupEditPage("boyakasha"))
        currentUrl shouldBe CacheGroupListPage.url
      }

      it("should allow me to edit a cache group") {
        val oldName = CacheGroupAddPage.createCacheGroup
        val newName = s"$oldName - updated"
        CacheGroupEditPage(oldName).updateWithName(newName)
        pageTitle should include("Cache group details")
        val descriptors = findAll(TagNameQuery("dd")).toSeq
        descriptors.find(_.text == newName) shouldBe 'defined
      }

    }

    describe("the delete CacheGroup page") {

      it("should redirect me to the cache group list page if no such cache group can be found") {
        goTo(CacheGroupDeletePage("whassaaaaap"))
        currentUrl shouldBe CacheGroupListPage.url
      }

      it("should allow me to reach the delete page") {
        val groupName = CacheGroupAddPage.createCacheGroup
        goTo(CacheGroupDeletePage(groupName))
        pageTitle should include("Delete")
      }

      it("should allow me to cancel, returning me back to the list page for cache groups") {
        val groupName = CacheGroupAddPage.createCacheGroup
        val deletePage = CacheGroupDeletePage(groupName)
        goTo(deletePage)
        deletePage.cancel()
        currentUrl shouldBe CacheGroupListPage.url
        val descriptors = findAll(TagNameQuery("td")).toSeq
        descriptors.find(_.text == groupName) shouldBe 'defined
      }

      it("should allow me to proceed with deletion, deleting the group and returning me back to the list page for cache groups") {
        val groupName = CacheGroupAddPage.createCacheGroup
        val deletePage = CacheGroupDeletePage(groupName)
        goTo(deletePage)
        deletePage.delete()
        currentUrl shouldBe CacheGroupListPage.url
        val descriptors = findAll(TagNameQuery("td")).toSeq
        descriptors.find(_.text == groupName) should not be 'defined
      }

    }

  }

}