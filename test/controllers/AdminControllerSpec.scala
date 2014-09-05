package controllers

import com.m3.octoparts.model.config._
import com.m3.octoparts.repository.MutableConfigsRepository
import com.m3.octoparts.repository.config._
import com.m3.octoparts.support.mocks.ConfigDataMocks
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatest.mock.MockitoSugar.mock
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future
import scala.language.postfixOps

class AdminController$Spec extends FlatSpec with Matchers {
  it should "append 3 underscores" in {
    AdminController.makeNewName("hello", Set("hello__", "hello", "hello_", "hi")) should equal("hello___")
  }
  it should "not append any star" in {
    AdminController.makeNewName("hello", Set.empty, "*") should equal("hello")
  }
}

class AdminControllerSpec extends FunSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with ConfigDataMocks {

  protected val aGroup = "some group"

  protected def part = {
    mockHttpPartConfig.copy(
      id = Some(3),
      hystrixConfig = Some(mockHystrixConfig.copy(
        threadPoolConfig = Some(mockThreadConfig),
        threadPoolConfigId = mockThreadConfig.id))
    )
  }

  protected val validPartEditFormParams: List[(String, String)] = List(
    "partId" -> "aNewName",
    "description" -> "stuff",
    "uri" -> "http://www.example2.com",
    "method" -> "post",
    "commandKey" -> "commandKey",
    "commandGroupKey" -> "commandGroupKey",
    "timeoutInMs" -> "5000",
    "threadPoolConfigId" -> "3"
  )

  it("should show a list of parts") {
    val repository = mock[MutableConfigsRepository]
    val adminController = new AdminController(repository = repository)
    doReturn(Future.successful(Seq(part.copy(uriToInterpolate = "http://www.example.com", owner = "aName")))).when(repository).findAllConfigs()

    val listParts = adminController.listParts(FakeRequest())
    contentAsString(listParts) should include("http://www.example.com")
    contentAsString(listParts) should include("aName")
  }

  it("should show a form to create a new part") {
    val repository = mock[MutableConfigsRepository]
    val adminController = new AdminController(repository = repository)

    doReturn(Future.successful(Seq(part))).when(repository).findAllConfigs()
    doReturn(Future.successful(Some(part))).when(repository).findConfigByPartId(part.partId)
    doReturn(Future.successful(Seq(mockThreadConfig))).when(repository).findAllThreadPoolConfigs()
    doReturn(Future.successful(Seq.empty)).when(repository).findAllCacheGroups()

    val result = adminController.newPart.apply(FakeRequest())
    status(result) should equal(200)
    contentAsString(result) should include("""input type="text" id="partId" name="partId" value="" """)
  }

  it("should show a form to edit an existing part") {
    val repository = mock[MutableConfigsRepository]
    val adminController = new AdminController(repository = repository)

    doReturn(Future.successful(Seq(part))).when(repository).findAllConfigs()
    doReturn(Future.successful(Some(part))).when(repository).findConfigByPartId(part.partId)
    doReturn(Future.successful(Seq(mockThreadConfig))).when(repository).findAllThreadPoolConfigs()
    doReturn(Future.successful(Seq.empty)).when(repository).findAllCacheGroups()

    val result = adminController.editPart(part.partId).apply(FakeRequest())
    status(result) should equal(200)
    contentAsString(result) should include(s"""input type="text" id="partId" name="partId" value="${part.partId}" """)
  }

  it("should create a new part") {
    val repository = mock[MutableConfigsRepository]
    val adminController = new AdminController(repository = repository)
    doReturn(Future.successful(124L)).when(repository).save(anyObject[HttpPartConfig]())(anyObject[ConfigMapper[HttpPartConfig]])
    doReturn(Future.successful(Seq.empty)).when(repository).findAllCacheGroupsByName(anyVararg[String]())
    doReturn(Future.successful(Seq(mockThreadConfig))).when(repository).findAllThreadPoolConfigs()
    doReturn(Future.successful(Seq.empty)).when(repository).findAllCacheGroups()

    val result = adminController.createPart.apply(FakeRequest().withFormUrlEncodedBody(validPartEditFormParams: _*))
    whenReady(result) { r =>
      status(result) should be(302)
      redirectLocation(result).get should include(routes.AdminController.showPart("aNewName").url)

      val newCiCaptor = ArgumentCaptor.forClass(classOf[HttpPartConfig])
      verify(repository).save(newCiCaptor.capture())(anyObject[ConfigMapper[HttpPartConfig]])
      verify(repository).findAllCacheGroupsByName(Seq.empty: _*)
      newCiCaptor.getValue.uriToInterpolate should be("http://www.example2.com")
    }
  }

  describe("when updating an existing part") {

    def setupController: (AdminController, MutableConfigsRepository, HttpPartConfig) = {
      val repository = mock[MutableConfigsRepository]
      val adminController = new AdminController(repository = repository)
      val part2 = part.copy(hystrixConfig = Some(mockHystrixConfig))
      doReturn(Future.successful(Some(part2))).when(repository).findConfigByPartId(part.partId)
      doReturn(Future.successful(1)).when(repository).deleteConfigByPartId(part.partId)
      doReturn(Future.successful(Seq.empty)).when(repository).findAllCacheGroupsByName(anyVararg[String]())
      doReturn(Future.successful(Some(mockPartParam))).when(repository).findParamById(anyLong())
      doReturn(Future.successful(Seq(mockThreadConfig))).when(repository).findAllThreadPoolConfigs()
      doReturn(Future.successful(Seq.empty)).when(repository).findAllCacheGroups()
      doReturn(Future.successful(Seq(part2))).when(repository).findAllConfigs()
      (adminController, repository, part2)
    }

    describe("when all is good with the world") {
      it("should update the item and redirect to the part's detail page") {
        val (controller, repository, part) = setupController
        doReturn(Future.successful(124L)).when(repository).save(anyObject[HttpPartConfig]())(anyObject[ConfigMapper[HttpPartConfig]])
        val result = controller.updatePart(part.partId).apply(FakeRequest().withFormUrlEncodedBody(validPartEditFormParams: _*))
        status(result) should be(302)
        redirectLocation(result).get should include(routes.AdminController.showPart("aNewName").url)
      }
    }

    describe("when the part doesn't exist") {
      it("should redirect to the part list page") {
        val (controller, repository, part) = setupController
        doReturn(Future.successful(None)).when(repository).findConfigByPartId("this is not here")

        val result = controller.updatePart("this is not here").apply(FakeRequest().withFormUrlEncodedBody(validPartEditFormParams: _*))
        status(result) should be(302)
        redirectLocation(result).get should include(routes.AdminController.listParts.url)
      }
    }

    describe("when the save fails") {
      it("should show the form again, with the user's filled-in information") {
        val (controller, repository, part) = setupController
        doReturn(Future.failed(new RuntimeException("oooh shiiieeet"))).when(repository).save(anyObject[HttpPartConfig]())(anyObject[ConfigMapper[HttpPartConfig]])

        val result = controller.updatePart(part.partId).apply(FakeRequest().withFormUrlEncodedBody(validPartEditFormParams: _*))
        status(result) should be(200)
        contentAsString(result) should include("""input type="text" id="partId" name="partId" value="aNewName" """)
      }
    }

  }

  describe("deleting a part") {
    describe("when all is well with the world") {
      it("should delete the part") {
        val repository = mock[MutableConfigsRepository]
        val adminController = new AdminController(repository = repository)
        doReturn(Future.successful(Some(part))).when(repository).findConfigByPartId(part.partId)
        doReturn(Future.successful(1)).when(repository).deleteConfigByPartId(part.partId)
        val deletePart = adminController.deletePart(part.partId)(FakeRequest())
        whenReady(deletePart) { result =>
          status(deletePart) should be(302)
          redirectLocation(deletePart).fold(fail())(_ should include(routes.AdminController.listParts.url))

          verify(repository).findConfigByPartId(part.partId)
          verify(repository).deleteConfigByPartId(part.partId)
        }
      }
    }

    describe("when the part does not exist") {
      it("should redirect to the list view with an error message") {
        val repository = mock[MutableConfigsRepository]
        val adminController = new AdminController(repository = repository)
        doReturn(Future.successful(None)).when(repository).findConfigByPartId("someOther")

        val deletePart = adminController.deletePart("someOther")(FakeRequest())
        whenReady(deletePart) { result =>
          status(deletePart) should be(302)
          redirectLocation(deletePart).fold(fail())(_ should include(routes.AdminController.listParts.url))
          flash(deletePart).get("Error") should be('defined)

          verify(repository).findConfigByPartId("someOther")
        }
      }
    }
  }

  describe("when copying a part") {
    describe("when all is well with the world") {
      it("should copy the part and show the edit form for the newly created part") {
        val repository = mock[MutableConfigsRepository]
        val adminController = new AdminController(repository = repository)
        doReturn(Future.successful(Seq(part))).when(repository).findAllConfigs()
        doReturn(Future.successful(Some(part))).when(repository).findConfigByPartId(part.partId)
        doReturn(Future.successful(124L)).when(repository).save(anyObject[HttpPartConfig]())(anyObject[ConfigMapper[HttpPartConfig]])

        val result = adminController.copyPart(part.partId).apply(FakeRequest())
        status(result) should be(302)
        val expectedNewPartId = part.partId + "_"
        redirectLocation(result).get should include(routes.AdminController.editPart(expectedNewPartId).url)
      }
    }

    describe("when the part does not exist") {
      it("should redirect to the list view with an error message") {
        val repository = mock[MutableConfigsRepository]
        val adminController = new AdminController(repository = repository)
        doReturn(Future.successful(None)).when(repository).findConfigByPartId("someOther")

        val deletePart = adminController.copyPart("someOther")(FakeRequest())
        whenReady(deletePart) { result =>
          status(deletePart) should be(302)
          redirectLocation(deletePart).fold(fail())(_ should include(routes.AdminController.listParts.url))
          flash(deletePart).get("Error") should be('defined)

          verify(repository).findConfigByPartId("someOther")
        }
      }
    }
  }

  describe("when copying a part") {
    describe("when all is well with the world") {
      it("should update the part with a new param based on the found param") {
        val repository = mock[MutableConfigsRepository]
        val adminController = new AdminController(repository = repository)

        doReturn(Future.successful(Some(part))).when(repository).findConfigByPartId(part.partId)
        doReturn(Future.successful(Some(mockPartParam))).when(repository).findParamById(mockPartParam.id.get)
        doReturn(Future.successful(37L)).when(repository).save(anyObject[PartParam]())(anyObject[ConfigMapper[PartParam]])

        val existingParam = part.parameters.head
        val copyParam = adminController.copyParam(part.partId, mockPartParam.id.get)(FakeRequest())
        whenReady(copyParam) { result =>
          status(copyParam) should equal(302)
          redirectLocation(copyParam).fold(fail())(_ should include(routes.AdminController.showPart(part.partId).url))

          verify(repository).findConfigByPartId(part.partId)
          verify(repository).findParamById(mockPartParam.id.get)
          val newCiCaptor = ArgumentCaptor.forClass(classOf[PartParam])
          verify(repository).save(newCiCaptor.capture())(anyObject[ConfigMapper[PartParam]])

          newCiCaptor.getValue.inputName should be(existingParam.inputName + "_")
          newCiCaptor.getValue.outputName should be(existingParam.outputName + "_")
          newCiCaptor.getValue.paramType should be(existingParam.paramType)
        }
      }
    }

    describe("when the part does not exist") {
      it("should redirect to the part detail view with an error message") {
        val repository = mock[MutableConfigsRepository]
        val adminController = new AdminController(repository = repository)

        doReturn(Future.successful(Some(part))).when(repository).findConfigByPartId(part.partId)
        doReturn(Future.successful(None)).when(repository).findParamById(anyLong())

        val copyParam = adminController.copyParam(part.partId, 12345L)(FakeRequest())
        whenReady(copyParam) { result =>
          status(copyParam) should equal(302)
          redirectLocation(copyParam).fold(fail())(_ should include(routes.AdminController.showPart(part.partId).url))

          verify(repository).findConfigByPartId(part.partId)
          verify(repository).findParamById(12345)
        }
      }
    }
  }

  it("should insert a new param") {
    val repository = mock[MutableConfigsRepository]
    val adminController = new AdminController(repository = repository)

    doReturn(Future.successful(Some(part))).when(repository).findConfigByPartId(part.partId)
    doReturn(Future.successful(37L)).when(repository).save(anyObject[PartParam]())(anyObject[ConfigMapper[PartParam]])
    doReturn(Future.successful(Seq.empty)).when(repository).findAllCacheGroupsByName(anyVararg[String]())

    val createParam = adminController.createParam(part.partId)(
      FakeRequest().withFormUrlEncodedBody("outputName" -> "someName", "paramType" -> "cookie")
    )
    whenReady(createParam) { result =>
      status(createParam) should equal(302)
      redirectLocation(createParam).fold(fail())(_ should include(routes.AdminController.showPart(part.partId).url))

      verify(repository).findConfigByPartId(part.partId)
      val newCiCaptor = ArgumentCaptor.forClass(classOf[PartParam])
      verify(repository).save(newCiCaptor.capture())(anyObject[ConfigMapper[PartParam]])
      verify(repository).findAllCacheGroupsByName(Seq.empty: _*)

      newCiCaptor.getValue.outputName should be("someName")
      newCiCaptor.getValue.paramType should be(ParamType.Cookie)
    }
  }

  it("should find and update an existing param") {
    val repository = mock[MutableConfigsRepository]
    val adminController = new AdminController(repository = repository)

    doReturn(Future.successful(Some(part))).when(repository).findConfigByPartId(part.partId)
    doReturn(Future.successful(37L)).when(repository).save(anyObject[PartParam]())(anyObject[ConfigMapper[PartParam]])
    doReturn(Future.successful(Some(mockPartParam))).when(repository).findParamById(mockPartParam.id.get)
    doReturn(Future.successful(Seq.empty)).when(repository).findAllCacheGroupsByName(anyVararg[String]())

    val updateParam = adminController.updateParam(part.partId, mockPartParam.id.get)(
      FakeRequest().withFormUrlEncodedBody("outputName" -> "newName", "paramType" -> "body")
    )
    whenReady(updateParam) { result =>
      status(updateParam) should equal(302)
      redirectLocation(updateParam).fold(fail())(_ should include(routes.AdminController.showPart(part.partId).url))

      verify(repository).findConfigByPartId(part.partId)
      val newCiCaptor = ArgumentCaptor.forClass(classOf[PartParam])
      verify(repository).save(newCiCaptor.capture())(anyObject[ConfigMapper[PartParam]])
      verify(repository).findAllCacheGroupsByName(Seq.empty: _*)

      newCiCaptor.getValue.outputName should be("newName")
      newCiCaptor.getValue.paramType should be(ParamType.Body)
    }
  }

  it("should add a new Thread Pool Config") {
    val repository = mock[MutableConfigsRepository]
    val adminController = new AdminController(repository = repository)
    doReturn(Future.successful(76L)).when(repository).save(anyObject[ThreadPoolConfig]())(anyObject[ConfigMapper[ThreadPoolConfig]])
    val createThreadPool = adminController.createThreadPool(
      FakeRequest().withFormUrlEncodedBody("threadPoolKey" -> "myNewThreadPool", "coreSize" -> "99")
    )
    whenReady(createThreadPool) { result =>
      status(createThreadPool) should be(302)
      val newCiCaptor = ArgumentCaptor.forClass(classOf[ThreadPoolConfig])
      verify(repository).save(newCiCaptor.capture())(anyObject[ConfigMapper[ThreadPoolConfig]])
      newCiCaptor.getValue.threadPoolKey should be("myNewThreadPool")
      newCiCaptor.getValue.coreSize should be(99)
    }
  }

  it("should update a thread pool config") {
    val repository = mock[MutableConfigsRepository]
    val adminController = new AdminController(repository = repository)
    doReturn(Future.successful(76L)).when(repository).save(anyObject[ThreadPoolConfig]())(anyObject[ConfigMapper[ThreadPoolConfig]])
    val tpc = mockThreadConfig.copy(id = Some(123))
    doReturn(Future.successful(Some(tpc))).when(repository).findThreadPoolConfigById(anyLong())

    val updateThreadPool = adminController.updateThreadPool(123L)(FakeRequest().withFormUrlEncodedBody("threadPoolKey" -> "myNewThreadPool", "coreSize" -> "99"))
    whenReady(updateThreadPool) { result =>
      verify(repository).findThreadPoolConfigById(123L)

      val newCiCaptor = ArgumentCaptor.forClass(classOf[ThreadPoolConfig])
      verify(repository).save(newCiCaptor.capture())(anyObject[ConfigMapper[ThreadPoolConfig]])
      newCiCaptor.getValue.threadPoolKey should be("myNewThreadPool")
      newCiCaptor.getValue.coreSize should be(99)
      newCiCaptor.getValue.createdAt should be(tpc.createdAt)
    }
  }

  it("should add a new CacheGroup") {
    val repository = mock[MutableConfigsRepository]
    val adminController = new AdminController(repository = repository)
    doReturn(Future.successful(76L)).when(repository).save(anyObject[CacheGroup]())(anyObject[ConfigMapper[CacheGroup]])

    val createCacheGroup = adminController.createCacheGroup(FakeRequest().withFormUrlEncodedBody("name" -> "newCacheGroup", "description" -> "hello"))
    whenReady(createCacheGroup) { result =>
      val newCiCaptor = ArgumentCaptor.forClass(classOf[CacheGroup])
      verify(repository).save(newCiCaptor.capture())(anyObject[ConfigMapper[CacheGroup]])
      newCiCaptor.getValue.name should be("newCacheGroup")
      newCiCaptor.getValue.description should be("hello")
    }
  }

  it("should update a CacheGroup") {
    val repository = mock[MutableConfigsRepository]
    val adminController = new AdminController(repository = repository)
    doReturn(Future.successful(76L)).when(repository).save(anyObject[CacheGroup]())(anyObject[ConfigMapper[CacheGroup]])
    doReturn(Future.successful(Seq(part))).when(repository).findAllConfigs()
    val cacheGroup = mockCacheGroup.copy(id = Some(123))
    doReturn(Future.successful(Some(cacheGroup))).when(repository).findCacheGroupByName(anyString())

    val saveCacheGroup = adminController.updateCacheGroup("123")(FakeRequest().withFormUrlEncodedBody("name" -> "myEditedThreadPool", "description" -> "harooo"))
    whenReady(saveCacheGroup) { result =>
      verify(repository).findCacheGroupByName("123")

      val newCiCaptor = ArgumentCaptor.forClass(classOf[CacheGroup])
      verify(repository).save(newCiCaptor.capture())(anyObject[ConfigMapper[CacheGroup]])
      newCiCaptor.getValue.name should be("myEditedThreadPool")
      newCiCaptor.getValue.description should be("harooo")
      newCiCaptor.getValue.createdAt should be(cacheGroup.createdAt)
    }
  }
}
