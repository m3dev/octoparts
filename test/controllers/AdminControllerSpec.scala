package controllers

import java.io.{ File, ByteArrayOutputStream }
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import com.m3.octoparts.cache.CacheOps
import com.m3.octoparts.cache.dummy.DummyCacheOps
import com.m3.octoparts.model.HttpMethod
import com.m3.octoparts.model.config._
import com.m3.octoparts.repository.MutableConfigsRepository
import com.m3.octoparts.repository.config._
import com.m3.octoparts.support.db.RequiresDB
import com.m3.octoparts.support.mocks.ConfigDataMocks
import com.twitter.zipkin.gen.Span
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.mockito.{ Mockito, ArgumentCaptor }
import org.mockito.Matchers._
import org.mockito.Matchers.{ eq => mockitoEq }
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatest.mock.MockitoSugar.mock
import play.api.http.Writeable
import play.api.libs.json.Json
import play.api.mvc.EssentialAction
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.mvc.Result

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class AdminControllerCompanionSpec extends FunSpec with Matchers with ConfigDataMocks {

  implicit val emptySpan = new Span()

  it("should append 3 underscores") {
    AdminController.makeNewName("hello", Set("hello__", "hello", "hello_", "hi")) should equal("hello___")
  }

  it("should not append any star") {
    AdminController.makeNewName("hello", Set.empty, "*") should equal("hello")
  }

  describe(".shouldBustCache(endpoint, endpoint)") {
    it("should return false if nothing changed at all") {
      AdminController.shouldBustCache(mockHttpPartConfig, mockHttpPartConfig) should be(false)
    }
    it("should return false if nothing important was changed") {
      AdminController.shouldBustCache(
        mockHttpPartConfig,
        mockHttpPartConfig.copy(alertMailsEnabled = !mockHttpPartConfig.alertMailsEnabled)) should be(false)
    }
    it("should return true if URI was changed ") {
      AdminController.shouldBustCache(
        mockHttpPartConfig,
        mockHttpPartConfig.copy(uriToInterpolate = s"${mockHttpPartConfig.uriToInterpolate}/whoa")) should be(true)
    }
    it("should return true if CacheTTL was changed to be shorter") {
      AdminController.shouldBustCache(
        mockHttpPartConfig.copy(cacheTtl = Some(3.second)),
        mockHttpPartConfig.copy(cacheTtl = Some(2.second))) should be(true)
    }
    it("should return true if CacheTTL was changed from None to Some(duration)") {
      AdminController.shouldBustCache(
        mockHttpPartConfig.copy(cacheTtl = None),
        mockHttpPartConfig.copy(cacheTtl = Some(1.second))) should be(true)
    }
    it("should return false if CacheTTL was changed to be longer") {
      AdminController.shouldBustCache(
        mockHttpPartConfig.copy(cacheTtl = Some(3.second)),
        mockHttpPartConfig.copy(cacheTtl = Some(4.second))) should be(false)
    }
    it("should return true if additionalValidStatuses was changed ") {
      AdminController.shouldBustCache(
        mockHttpPartConfig,
        mockHttpPartConfig.copy(additionalValidStatuses = mockHttpPartConfig.additionalValidStatuses + 911)) should be(true)
    }
    it("should return true if method was changed ") {
      AdminController.shouldBustCache(
        mockHttpPartConfig,
        mockHttpPartConfig.copy(method = HttpMethod.values.filter(_ != mockHttpPartConfig.method).firstKey)) should be(true)
    }
  }

  describe(".shouldBustCache(param)") {
    val unremarkableParam = mockPartParam.copy(required = false, versioned = false)
    it("should return false the param is neither required nor versioned") {
      AdminController.shouldBustCache(unremarkableParam) should be(false)
    }
    it("should return true if the param is required") {
      AdminController.shouldBustCache(unremarkableParam.copy(required = true)) should be(true)
    }
    it("should return true if the param is versioned") {
      AdminController.shouldBustCache(unremarkableParam.copy(versioned = true)) should be(true)
    }
  }
}

class AdminControllerSpec extends FunSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with ConfigDataMocks
    with RequiresDB {

  implicit val emptySpan = new Span()

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
    "httpSettings.uri" -> "http://www.example2.com",
    "httpSettings.method" -> "post",
    "httpSettings.httpPoolSize" -> "5",
    "httpSettings.httpConnectionTimeoutInMs" -> "1000",
    "httpSettings.httpSocketTimeoutInMs" -> "5000",
    "httpSettings.httpDefaultEncoding" -> "UTF-8",
    "hystrixConfig.commandKey" -> "commandKey",
    "hystrixConfig.commandGroupKey" -> "commandGroupKey",
    "hystrixConfig.timeoutInMs" -> "5000",
    "hystrixConfig.threadPoolConfigId" -> "3",
    "hystrixConfig.localContentsAsFallback" -> "true"
  )

  it("should show a list of parts") {
    val repository = mock[MutableConfigsRepository]
    val adminController = new AdminController(cacheOps = DummyCacheOps, repository = repository)
    doReturn(Future.successful(Seq(part.copy(uriToInterpolate = "http://www.example.com", owner = "aName")))).when(repository).findAllConfigs()

    val listParts = adminController.listParts(FakeRequest())
    contentAsString(listParts) should include("http://www.example.com")
    contentAsString(listParts) should include("aName")
  }

  it("should show a form to create a new part") {
    val repository = mock[MutableConfigsRepository]
    val adminController = new AdminController(cacheOps = DummyCacheOps, repository = repository)

    doReturn(Future.successful(Seq(part))).when(repository).findAllConfigs()
    doReturn(Future.successful(Some(part))).when(repository).findConfigByPartId(part.partId)
    doReturn(Future.successful(Seq(mockThreadConfig))).when(repository).findAllThreadPoolConfigs()
    doReturn(Future.successful(Seq.empty)).when(repository).findAllCacheGroups()

    val result = adminController.newPart.apply(FakeRequest())
    status(result) should equal(OK)
    contentAsString(result) should include("""input type="text" id="partId" name="partId" value="" """)
  }

  it("should show a form to edit an existing part") {
    val repository = mock[MutableConfigsRepository]
    val adminController = new AdminController(cacheOps = DummyCacheOps, repository = repository)

    doReturn(Future.successful(Seq(part))).when(repository).findAllConfigs()
    doReturn(Future.successful(Some(part))).when(repository).findConfigByPartId(part.partId)
    doReturn(Future.successful(Seq(mockThreadConfig))).when(repository).findAllThreadPoolConfigs()
    doReturn(Future.successful(Seq.empty)).when(repository).findAllCacheGroups()

    val result = adminController.editPart(part.partId).apply(FakeRequest())
    status(result) should equal(OK)
    contentAsString(result) should include(s"""input type="text" id="partId" name="partId" value="${part.partId}" """)
  }

  it("should create a new part") {
    val repository = mock[MutableConfigsRepository]
    val adminController = new AdminController(cacheOps = DummyCacheOps, repository = repository)
    doReturn(Future.successful(124L)).when(repository).save(anyObject[HttpPartConfig]())(anyObject[ConfigMapper[HttpPartConfig]], anyObject[Span])
    doReturn(Future.successful(Seq.empty)).when(repository).findAllCacheGroupsByName(anyVararg[String]())(anyObject[Span])
    doReturn(Future.successful(Seq(mockThreadConfig))).when(repository).findAllThreadPoolConfigs()
    doReturn(Future.successful(Seq.empty)).when(repository).findAllCacheGroups()

    val result = adminController.createPart.apply(FakeRequest().withFormUrlEncodedBody(validPartEditFormParams: _*))
    whenReady(result) { r =>
      status(result) should be(FOUND)
      redirectLocation(result).get should include(routes.AdminController.showPart("aNewName").url)

      val newCiCaptor = ArgumentCaptor.forClass(classOf[HttpPartConfig])
      verify(repository).save(newCiCaptor.capture())(anyObject[ConfigMapper[HttpPartConfig]], anyObject[Span])
      verify(repository).findAllCacheGroupsByName(Seq.empty: _*)
      newCiCaptor.getValue.uriToInterpolate should be("http://www.example2.com")
    }
  }

  describe("when updating an existing part") {

    def setupController: (AdminController, CacheOps, MutableConfigsRepository, HttpPartConfig) = {
      val repository = mock[MutableConfigsRepository]
      val cacheOps = mock[CacheOps]
      val adminController = new AdminController(cacheOps = cacheOps, repository = repository)
      val part2 = part.copy(hystrixConfig = Some(mockHystrixConfig))
      doReturn(Future.successful(())).when(cacheOps).increasePartVersion(anyString())(anyObject[Span])
      doReturn(Future.successful(Some(part2))).when(repository).findConfigByPartId(mockitoEq(part.partId))(anyObject[Span])
      doReturn(Future.successful(1)).when(repository).deleteConfigByPartId(mockitoEq(part.partId))(anyObject[Span])
      doReturn(Future.successful(Seq.empty)).when(repository).findAllCacheGroupsByName(anyVararg[String]())(anyObject[Span])
      doReturn(Future.successful(Some(mockPartParam))).when(repository).findParamById(anyLong())(anyObject[Span])
      doReturn(Future.successful(Seq(mockThreadConfig))).when(repository).findAllThreadPoolConfigs()(anyObject[Span])
      doReturn(Future.successful(Seq.empty)).when(repository).findAllCacheGroups()(anyObject[Span])
      doReturn(Future.successful(Seq(part2))).when(repository).findAllConfigs()(anyObject[Span])
      (adminController, cacheOps, repository, part2)
    }

    describe("when all is good with the world") {
      it("should update the item and redirect to the part's detail page") {
        val (controller, cacheOps, repository, part) = setupController
        doReturn(Future.successful(124L)).when(repository).save(anyObject[HttpPartConfig]())(anyObject[ConfigMapper[HttpPartConfig]], anyObject[Span])
        val result = controller.updatePart(part.partId).apply(FakeRequest().withFormUrlEncodedBody(validPartEditFormParams: _*))
        status(result) should be(FOUND)
        redirectLocation(result).get should include(routes.AdminController.showPart("aNewName").url)
        verify(cacheOps, Mockito.timeout(1000)).increasePartVersion(part.partId)
      }
    }

    describe("when the part doesn't exist") {
      it("should redirect to the part list page") {
        val (controller, _, repository, part) = setupController
        doReturn(Future.successful(None)).when(repository).findConfigByPartId(mockitoEq("this is not here"))(anyObject[Span])

        val result = controller.updatePart("this is not here").apply(FakeRequest().withFormUrlEncodedBody(validPartEditFormParams: _*))
        status(result) should be(FOUND)
        redirectLocation(result).get should include(routes.AdminController.listParts.url)
      }
    }

    describe("when the save fails") {
      it("should show the form again, with the user's filled-in information") {
        val (controller, _, repository, part) = setupController
        doReturn(Future.failed(new RuntimeException("oooh shiiieeet"))).when(repository).save(anyObject[HttpPartConfig]())(anyObject[ConfigMapper[HttpPartConfig]], anyObject[Span])

        val result = controller.updatePart(part.partId).apply(FakeRequest().withFormUrlEncodedBody(validPartEditFormParams: _*))
        status(result) should be(OK)
        contentAsString(result) should include("""input type="text" id="partId" name="partId" value="aNewName" """)
      }
    }

  }

  describe("importing parts") {
    it("should show the form") {
      val repository = mock[MutableConfigsRepository]
      val adminController = new AdminController(cacheOps = DummyCacheOps, repository = repository)
      val showImportParts = adminController.showImportParts()(FakeRequest())
      status(showImportParts) should be(OK)
      contentAsString(showImportParts) should include("jsonfile")
      contentAsString(showImportParts) should include(routes.AdminController.doImportParts().url)
      verifyNoMoreInteractions(repository)
    }

    def makeFileUpload(data: Array[Byte], action: EssentialAction, fileKey: String = "jsonfile", fileContentType: ContentType = ContentType.APPLICATION_JSON): Future[Result] = {

      val jsonFile = {
        val tmpFile = File.createTempFile("upload", "tmp")
        tmpFile.deleteOnExit()
        Files.write(tmpFile.toPath, data)
        tmpFile
      }
      val entity = MultipartEntityBuilder
        .create()
        .addPart(fileKey, new FileBody(jsonFile, fileContentType, jsonFile.getName))
        .build()

      val postReq = FakeRequest("POST", "ignored").withBody {
        val outputStream = new ByteArrayOutputStream()
        entity.writeTo(outputStream)
        outputStream.toByteArray
      }.withHeaders(CONTENT_TYPE -> entity.getContentType.getValue)

      // override Play's equivalent Writeable so that the content-type header from the FakeRequest is used instead of application/octet-stream
      call(action, postReq)(Writeable(identity, None))
    }

    it("should import a valid JSON file") {
      import com.m3.octoparts.json.format.ConfigModel._

      val repository = mock[MutableConfigsRepository]
      val adminController = new AdminController(cacheOps = DummyCacheOps, repository = repository)
      val jsonParts = Seq(HttpPartConfig.toJsonModel(part))
      doReturn(Future.successful(Seq(part.partId))).when(repository).importConfigs(jsonParts)
      val data = Json.toJson(jsonParts).toString()
      val doImportParts = makeFileUpload(data.getBytes(StandardCharsets.UTF_8), adminController.doImportParts())
      status(doImportParts) should be(FOUND)
      redirectLocation(doImportParts).getOrElse(fail()) should include(routes.AdminController.listParts().url)
      flash(doImportParts).get(BootstrapFlashStyles.danger.toString) should be(None)
      flash(doImportParts).get(BootstrapFlashStyles.success.toString) shouldNot be(None)
      verify(repository).importConfigs(jsonParts)
      verifyNoMoreInteractions(repository)
    }

    it("could never import a broken JSON file") {
      val repository = mock[MutableConfigsRepository]
      val adminController = new AdminController(cacheOps = DummyCacheOps, repository = repository)
      val doImportParts = makeFileUpload("INVALID JSON".getBytes(StandardCharsets.UTF_8), adminController.doImportParts())
      status(doImportParts) should be(FOUND)
      redirectLocation(doImportParts).getOrElse(fail()) should include(routes.AdminController.listParts().url)
      flash(doImportParts).get(BootstrapFlashStyles.danger.toString) shouldNot be(None)
      verifyNoMoreInteractions(repository)
    }
  }

  describe("deleting a part") {
    describe("when all is well with the world") {
      it("should delete the part") {
        val repository = mock[MutableConfigsRepository]
        val cacheOps = mock[CacheOps]
        when(cacheOps.increasePartVersion(anyString())(anyObject[Span])).thenReturn(Future.successful(()))
        val adminController = new AdminController(cacheOps = cacheOps, repository = repository)
        doReturn(Future.successful(Some(part))).when(repository).findConfigByPartId(mockitoEq(part.partId))(anyObject[Span])
        doReturn(Future.successful(1)).when(repository).deleteConfigByPartId(mockitoEq(part.partId))(anyObject[Span])
        val deletePart = adminController.deletePart(part.partId)(FakeRequest())
        whenReady(deletePart) { result =>
          status(deletePart) should be(FOUND)
          redirectLocation(deletePart).fold(fail())(_ should include(routes.AdminController.listParts().url))

          verify(repository).findConfigByPartId(mockitoEq(part.partId))(anyObject[Span])
          verify(repository).deleteConfigByPartId(mockitoEq(part.partId))(anyObject[Span])
          verify(cacheOps, Mockito.timeout(1000)).increasePartVersion(mockitoEq(part.partId))(anyObject[Span])
        }
      }
    }

    describe("when the part does not exist") {
      it("should redirect to the list view with an error message") {
        val repository = mock[MutableConfigsRepository]
        val adminController = new AdminController(cacheOps = DummyCacheOps, repository = repository)
        doReturn(Future.successful(None)).when(repository).findConfigByPartId(mockitoEq("someOther"))(anyObject[Span])

        val deletePart = adminController.deletePart("someOther")(FakeRequest())
        whenReady(deletePart) { result =>
          status(deletePart) should be(FOUND)
          redirectLocation(deletePart).fold(fail())(_ should include(routes.AdminController.listParts.url))
          flash(deletePart).get(BootstrapFlashStyles.danger.toString) should be('defined)

          verify(repository).findConfigByPartId(mockitoEq("someOther"))(anyObject[Span])
        }
      }
    }
  }

  describe("when copying a part") {
    describe("when all is well with the world") {
      it("should copy the part and show the edit form for the newly created part") {
        val repository = mock[MutableConfigsRepository]
        val adminController = new AdminController(cacheOps = DummyCacheOps, repository = repository)
        doReturn(Future.successful(Seq(part))).when(repository).findAllConfigs()(anyObject[Span])
        doReturn(Future.successful(Some(part))).when(repository).findConfigByPartId(mockitoEq(part.partId))(anyObject[Span])
        doReturn(Future.successful(124L)).when(repository).save(anyObject[HttpPartConfig]())(anyObject[ConfigMapper[HttpPartConfig]], anyObject[Span])

        val result = adminController.copyPart(part.partId).apply(FakeRequest())
        status(result) should be(FOUND)
        val expectedNewPartId = part.partId + "_"
        redirectLocation(result).get should include(routes.AdminController.editPart(expectedNewPartId).url)
      }
    }

    describe("when the part does not exist") {
      it("should redirect to the list view with an error message") {
        val repository = mock[MutableConfigsRepository]
        val adminController = new AdminController(cacheOps = DummyCacheOps, repository = repository)
        doReturn(Future.successful(None)).when(repository).findConfigByPartId(mockitoEq("someOther"))(anyObject[Span])

        val deletePart = adminController.copyPart("someOther")(FakeRequest())
        whenReady(deletePart) { result =>
          status(deletePart) should be(FOUND)
          redirectLocation(deletePart).fold(fail())(_ should include(routes.AdminController.listParts.url))
          flash(deletePart).get(BootstrapFlashStyles.danger.toString) should be('defined)

          verify(repository).findConfigByPartId("someOther")
        }
      }
    }
  }

  describe("when copying a part") {
    describe("when all is well with the world") {
      it("should update the part with a new param based on the found param") {
        val repository = mock[MutableConfigsRepository]
        val adminController = new AdminController(cacheOps = DummyCacheOps, repository = repository)

        doReturn(Future.successful(Some(part))).when(repository).findConfigByPartId(mockitoEq(part.partId))(anyObject[Span])
        doReturn(Future.successful(Some(mockPartParam))).when(repository).findParamById(mockitoEq(mockPartParam.id.get))(anyObject[Span])
        doReturn(Future.successful(37L)).when(repository).save(anyObject[PartParam]())(anyObject[ConfigMapper[PartParam]], anyObject[Span])

        val existingParam = part.parameters.head
        val copyParam = adminController.copyParam(part.partId, mockPartParam.id.get)(FakeRequest())
        whenReady(copyParam) { result =>
          status(copyParam) should equal(FOUND)
          redirectLocation(copyParam).fold(fail())(_ should include(routes.AdminController.showPart(part.partId).url))

          verify(repository).findConfigByPartId(part.partId)
          verify(repository).findParamById(mockPartParam.id.get)
          val newCiCaptor = ArgumentCaptor.forClass(classOf[PartParam])
          verify(repository).save(newCiCaptor.capture())(anyObject[ConfigMapper[PartParam]], anyObject[Span])

          newCiCaptor.getValue.inputName should be(existingParam.inputName + "_")
          newCiCaptor.getValue.outputName should be(existingParam.outputName + "_")
          newCiCaptor.getValue.paramType should be(existingParam.paramType)
        }
      }
    }

    describe("when the part does not exist") {
      it("should redirect to the part detail view with an error message") {
        val repository = mock[MutableConfigsRepository]
        val adminController = new AdminController(cacheOps = DummyCacheOps, repository = repository)

        doReturn(Future.successful(Some(part))).when(repository).findConfigByPartId(mockitoEq(part.partId))(anyObject[Span])
        doReturn(Future.successful(None)).when(repository).findParamById(anyLong())(anyObject[Span])

        val copyParam = adminController.copyParam(part.partId, 12345L)(FakeRequest())
        whenReady(copyParam) { result =>
          status(copyParam) should equal(FOUND)
          redirectLocation(copyParam).fold(fail())(_ should include(routes.AdminController.showPart(part.partId).url))

          verify(repository).findConfigByPartId(part.partId)
          verify(repository).findParamById(12345)
        }
      }
    }
  }

  it("should insert a new param") {
    val repository = mock[MutableConfigsRepository]
    val cacheOps = mock[CacheOps]
    when(cacheOps.increasePartVersion(anyString())(anyObject[Span])).thenReturn(Future.successful(()))
    val adminController = new AdminController(cacheOps = cacheOps, repository = repository)

    doReturn(Future.successful(Some(part))).when(repository).findConfigByPartId(mockitoEq(part.partId))(anyObject[Span])
    doReturn(Future.successful(37L)).when(repository).save(anyObject[PartParam]())(anyObject[ConfigMapper[PartParam]], anyObject[Span])
    doReturn(Future.successful(Seq.empty)).when(repository).findAllCacheGroupsByName(anyVararg[String]())(anyObject[Span])

    val createParam = adminController.createParam(part.partId)(
      FakeRequest().withFormUrlEncodedBody("outputName" -> "someName", "paramType" -> "cookie", "required" -> "true")
    )
    whenReady(createParam) { result =>
      status(createParam) should equal(FOUND)
      redirectLocation(createParam).fold(fail())(_ should include(routes.AdminController.showPart(part.partId).url))

      verify(repository).findConfigByPartId(part.partId)
      val newCiCaptor = ArgumentCaptor.forClass(classOf[PartParam])
      verify(repository).save(newCiCaptor.capture())(anyObject[ConfigMapper[PartParam]], anyObject[Span])
      verify(repository).findAllCacheGroupsByName(Seq.empty: _*)
      verify(cacheOps, Mockito.timeout(1000)).increasePartVersion(part.partId)

      newCiCaptor.getValue.outputName should be("someName")
      newCiCaptor.getValue.paramType should be(ParamType.Cookie)
    }
  }

  it("should find and update an existing param") {
    val repository = mock[MutableConfigsRepository]
    val cacheOps = mock[CacheOps]
    when(cacheOps.increasePartVersion(anyString())(anyObject[Span])).thenReturn(Future.successful(()))
    val adminController = new AdminController(cacheOps = cacheOps, repository = repository)

    doReturn(Future.successful(Some(part))).when(repository).findConfigByPartId(part.partId)
    doReturn(Future.successful(37L)).when(repository).save(anyObject[PartParam]())(anyObject[ConfigMapper[PartParam]], anyObject[Span])
    doReturn(Future.successful(Some(mockPartParam))).when(repository).findParamById(mockitoEq(mockPartParam.id.get))(anyObject[Span])
    doReturn(Future.successful(Seq.empty)).when(repository).findAllCacheGroupsByName(anyVararg[String]())(anyObject[Span])

    val updateParam = adminController.updateParam(part.partId, mockPartParam.id.get)(
      FakeRequest().withFormUrlEncodedBody("outputName" -> "newName", "paramType" -> "body", "versioned" -> "true")
    )
    whenReady(updateParam) { result =>
      status(updateParam) should equal(FOUND)
      redirectLocation(updateParam).fold(fail())(_ should include(routes.AdminController.showPart(part.partId).url))

      verify(repository).findConfigByPartId(part.partId)
      val newCiCaptor = ArgumentCaptor.forClass(classOf[PartParam])
      verify(repository).save(newCiCaptor.capture())(anyObject[ConfigMapper[PartParam]], anyObject[Span])
      verify(repository).findAllCacheGroupsByName(Seq.empty: _*)
      verify(cacheOps, Mockito.timeout(1000)).increasePartVersion(part.partId)

      newCiCaptor.getValue.outputName should be("newName")
      newCiCaptor.getValue.paramType should be(ParamType.Body)
    }
  }

  it("should delete an existing param") {
    val repository = mock[MutableConfigsRepository]
    val cacheOps = mock[CacheOps]
    when(cacheOps.increasePartVersion(anyString())(anyObject[Span])).thenReturn(Future.successful(()))
    val adminController = new AdminController(cacheOps = cacheOps, repository = repository)
    when(repository.deletePartParamById(anyLong())(anyObject[Span])).thenReturn(Future.successful(1))
    doReturn(Future.successful(Some(part))).when(repository).findConfigByPartId(part.partId)
    doReturn(Future.successful(Some(mockPartParam))).when(repository).findParamById(mockitoEq(mockPartParam.id.get))(anyObject[Span])
    doReturn(Future.successful(Seq.empty)).when(repository).findAllCacheGroupsByName(anyVararg[String]())(anyObject[Span])

    val deleteParam = adminController.deleteParam(part.partId, mockPartParam.id.get)(FakeRequest())
    status(deleteParam) should equal(FOUND)
    redirectLocation(deleteParam).fold(fail())(_ should include(routes.AdminController.showPart(part.partId).url))
    whenReady(deleteParam) { result =>
      verify(repository).findConfigByPartId(part.partId)
      val newCiCaptor = ArgumentCaptor.forClass(classOf[Long])
      verify(repository).deletePartParamById(newCiCaptor.capture())(anyObject[Span])
      verify(cacheOps, Mockito.timeout(1000)).increasePartVersion(mockitoEq(part.partId))(anyObject[Span])
      newCiCaptor.getValue should be(part.id.get)
    }
  }

  it("should display a ThreadPoolConfig") {
    val repository = mock[MutableConfigsRepository]
    val adminController = new AdminController(cacheOps = DummyCacheOps, repository = repository)
    val tpc = mockThreadConfig.copy(id = Some(123L))
    doReturn(Future.successful(Some(tpc))).when(repository).findThreadPoolConfigById(mockitoEq(123L))(anyObject[Span])

    val showThreadPool = adminController.showThreadPool(123L)(FakeRequest())
    whenReady(showThreadPool) { result =>
      verify(repository).findThreadPoolConfigById(mockitoEq(123L))(anyObject[Span])
      result.header.status shouldBe OK
    }
  }

  it("should add a new Thread Pool Config") {
    val repository = mock[MutableConfigsRepository]
    val adminController = new AdminController(cacheOps = DummyCacheOps, repository = repository)
    doReturn(Future.successful(76L)).when(repository).save(anyObject[ThreadPoolConfig]())(anyObject[ConfigMapper[ThreadPoolConfig]], anyObject[Span])
    val createThreadPool = adminController.createThreadPool(
      FakeRequest().withFormUrlEncodedBody("threadPoolKey" -> "myNewThreadPool", "coreSize" -> "99")
    )
    whenReady(createThreadPool) { result =>
      status(createThreadPool) should be(FOUND)
      val newCiCaptor = ArgumentCaptor.forClass(classOf[ThreadPoolConfig])
      verify(repository).save(newCiCaptor.capture())(anyObject[ConfigMapper[ThreadPoolConfig]], anyObject[Span])
      newCiCaptor.getValue.threadPoolKey should be("myNewThreadPool")
      newCiCaptor.getValue.coreSize should be(99)
    }
  }

  it("should update a thread pool config") {
    val repository = mock[MutableConfigsRepository]
    val adminController = new AdminController(cacheOps = DummyCacheOps, repository = repository)
    val tpc = mockThreadConfig.copy(id = Some(123L))
    doReturn(Future.successful(Some(tpc))).when(repository).findThreadPoolConfigById(anyLong())(anyObject[Span])
    doReturn(Future.successful(123L)).when(repository).save(anyObject[ThreadPoolConfig]())(anyObject[ConfigMapper[ThreadPoolConfig]], anyObject[Span])

    val updateThreadPool = adminController.updateThreadPool(123L)(FakeRequest().withFormUrlEncodedBody("threadPoolKey" -> "myNewThreadPool", "coreSize" -> "99"))
    whenReady(updateThreadPool) { result =>
      verify(repository).findThreadPoolConfigById(123L)

      val newCiCaptor = ArgumentCaptor.forClass(classOf[ThreadPoolConfig])
      verify(repository).save(newCiCaptor.capture())(anyObject[ConfigMapper[ThreadPoolConfig]], anyObject[Span])
      newCiCaptor.getValue.threadPoolKey should be("myNewThreadPool")
      newCiCaptor.getValue.coreSize should be(99)
      newCiCaptor.getValue.createdAt should be(tpc.createdAt)
    }
  }

  it("should add a new CacheGroup") {
    val repository = mock[MutableConfigsRepository]
    val adminController = new AdminController(cacheOps = DummyCacheOps, repository = repository)
    doReturn(Future.successful(76L)).when(repository).save(anyObject[CacheGroup]())(anyObject[ConfigMapper[CacheGroup]], anyObject[Span])

    val createCacheGroup = adminController.createCacheGroup(FakeRequest().withFormUrlEncodedBody("name" -> "newCacheGroup", "description" -> "hello"))
    whenReady(createCacheGroup) { result =>
      val newCiCaptor = ArgumentCaptor.forClass(classOf[CacheGroup])
      verify(repository).save(newCiCaptor.capture())(anyObject[ConfigMapper[CacheGroup]], anyObject[Span])
      newCiCaptor.getValue.name should be("newCacheGroup")
      newCiCaptor.getValue.description should be("hello")
    }
  }

  it("should update a CacheGroup") {
    val repository = mock[MutableConfigsRepository]
    val adminController = new AdminController(cacheOps = DummyCacheOps, repository = repository)
    doReturn(Future.successful(76L)).when(repository).save(anyObject[CacheGroup]())(anyObject[ConfigMapper[CacheGroup]], anyObject[Span])
    doReturn(Future.successful(Seq(part))).when(repository).findAllConfigs()(anyObject[Span])
    val cacheGroup = mockCacheGroup.copy(id = Some(123))
    doReturn(Future.successful(Some(cacheGroup))).when(repository).findCacheGroupByName(anyString())(anyObject[Span])

    val saveCacheGroup = adminController.updateCacheGroup("123")(FakeRequest().withFormUrlEncodedBody("name" -> "myEditedThreadPool", "description" -> "harooo"))
    whenReady(saveCacheGroup) { result =>
      verify(repository).findCacheGroupByName("123")

      val newCiCaptor = ArgumentCaptor.forClass(classOf[CacheGroup])
      verify(repository).save(newCiCaptor.capture())(anyObject[ConfigMapper[CacheGroup]], anyObject[Span])
      newCiCaptor.getValue.name should be("myEditedThreadPool")
      newCiCaptor.getValue.description should be("harooo")
      newCiCaptor.getValue.createdAt should be(cacheGroup.createdAt)
    }
  }
}
