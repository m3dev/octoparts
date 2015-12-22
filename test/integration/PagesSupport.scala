package integration

import com.m3.octoparts.model.config.json.ThreadPoolConfig
import com.m3.octoparts.support.{ PlayServerSupport, OneDIBrowserPerSuite }
import org.scalatest.selenium.{ Page => SeleniumPage }
import org.openqa.selenium.support.ui.Select

trait PagesSupport { this: OneDIBrowserPerSuite with PlayServerSupport =>

  type ThreadPoolId = Long

  lazy val baseUrl: String = s"http://localhost:$port"

  object ThreadPoolListPage extends SeleniumPage {
    val url: String = s"$baseUrl/admin/thread-pools"
  }

  case class ThreadPoolShowPage(threadPoolId: ThreadPoolId) extends SeleniumPage {
    val url: String = s"$baseUrl/admin/thread-pools/$threadPoolId/show"
  }

  object ThreadPoolAddPage extends SeleniumPage {

    val url: String = s"$baseUrl/admin/thread-pools/new"

    def createThreadPool: (ThreadPoolId, ThreadPoolConfig) = {
      val name = s"my little pool $uniqueId"
      val size = 10
      val queueSize = 500
      goTo(ThreadPoolAddPage)
      textField("threadPoolKey").value = name
      numberField("coreSize").value = s"$size"
      numberField("queueSize").value = s"$queueSize"
      submit()
      val id = currentUrl.split("/").init.last.toLong
      (id, ThreadPoolConfig(name, size, queueSize))
    }

  }

  case class ThreadPoolDeletePage(threadPoolId: ThreadPoolId) extends SeleniumPage {
    val url: String = s"$baseUrl/admin/thread-pools/$threadPoolId/delete"

    def delete() = {
      find(TagNameQuery("input")).filter(_.attribute("value").contains("Delete")).foreach(clickOn)
    }

    def cancel() = click on linkText("Cancel")
  }

  case class ThreadPoolEditPage(threadPoolId: ThreadPoolId) extends SeleniumPage {
    val url: String = s"$baseUrl/admin/thread-pools/$threadPoolId/edit"

    def updateWithName(name: String) = {
      goTo(this)
      textField("threadPoolKey").value = name
      submit()
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

}