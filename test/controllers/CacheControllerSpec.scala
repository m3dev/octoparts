package controllers

import com.m3.octoparts.cache.CacheOps
import com.m3.octoparts.cache.versioning.VersionedParamKey
import com.m3.octoparts.model.config.CacheGroup
import com.m3.octoparts.repository.ConfigsRepository
import com.m3.octoparts.support.PlayAppSupport
import com.m3.octoparts.support.mocks.ConfigDataMocks
import com.twitter.zipkin.gen.Span
import org.mockito.Matchers.{ eq => mockitoEq, _ }
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ FlatSpec, Matchers }
import play.api.http.FileMimeTypes
import play.api.i18n.{ Langs, MessagesApi }
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.collection.SortedSet
import scala.concurrent.Future
import scala.language.postfixOps

class CacheControllerSpec extends FlatSpec with Matchers with MockitoSugar with ConfigDataMocks with ScalaFutures with PlayAppSupport {
  private implicit val emptySpan = new Span()

  private def partCacheGroup: CacheGroup = mockCacheGroup.copy(
    name = "group1",
    httpPartConfigs = SortedSet(mockHttpPartConfig, mockHttpPartConfig.copy(partId = "another"))
  )
  private def paramCacheGroup: CacheGroup = mockCacheGroup.copy(
    name = "group2",
    partParams = SortedSet(
      mockPartParam.copy(httpPartConfig = Some(mockHttpPartConfig)),
      mockPartParam.copy(outputName = "another param", httpPartConfig = Some(mockHttpPartConfig.copy(partId = "another")))
    )
  )

  it should "return 200 and call increasePartVersion with the partId when /invalidate/:partId is called" in {
    val mockCacheOps = mock[CacheOps](RETURNS_SMART_NULLS)
    val mockRepository = mock[ConfigsRepository](RETURNS_SMART_NULLS)
    val controller = new CacheController(mockCacheOps, mockRepository, appComponents.controllerComponents)
    doReturn(Future.successful(Unit)).when(mockCacheOps).increasePartVersion(anyString())(anyObject[Span])
    whenReady(controller.invalidatePart("part1").apply(FakeRequest())) { result =>
      verify(mockCacheOps).increasePartVersion("part1")
      result.header.status should be(OK)
    }
  }

  it should
    """
      |return 200 and call increaseParamVersion with the partId, param name, and param value when
      |/invalidate/:partId/:paramName/:paramValue is called""".stripMargin in {
      val mockCacheOps = mock[CacheOps](RETURNS_SMART_NULLS)
      val mockRepository = mock[ConfigsRepository](RETURNS_SMART_NULLS)
      val controller = new CacheController(mockCacheOps, mockRepository, appComponents.controllerComponents)
      doReturn(Future.successful(Unit)).when(mockCacheOps).increaseParamVersion(anyObject[VersionedParamKey]())(anyObject[Span])
      whenReady(controller.invalidatePartParam("part1", "paramName", "paramValue")(FakeRequest())) { result =>
        verify(mockCacheOps).increaseParamVersion(VersionedParamKey("part1", "paramName", "paramValue"))
        verifyNoMoreInteractions(mockCacheOps)
      }
    }

  it should "return 404 when /invalidate/cacheGroup/:cacheGroupName is called with a non existent cacheGroupName" in {
    val mockCacheOps = mock[CacheOps](RETURNS_SMART_NULLS)
    val mockRepository = mock[ConfigsRepository](RETURNS_SMART_NULLS)
    val controller = new CacheController(mockCacheOps, mockRepository, appComponents.controllerComponents)
    doReturn(Future.successful(None)).when(mockRepository).findCacheGroupByName(mockitoEq("wutthewut"))(anyObject[Span])
    whenReady(controller.invalidateCacheGroupParts("wutthewut")(FakeRequest())) { result =>
      result.header.status should be(NOT_FOUND)
      verifyNoMoreInteractions(mockCacheOps)
    }
  }

  it should "return 200 when /invalidate/cacheGroup/:cacheGroupName is called with an existing cacheGroupName" in {
    val mockCacheOps = mock[CacheOps](RETURNS_SMART_NULLS)
    val mockRepository = mock[ConfigsRepository](RETURNS_SMART_NULLS)
    val controller = new CacheController(mockCacheOps, mockRepository, appComponents.controllerComponents)
    doReturn(Future.successful(Some(partCacheGroup))).when(mockRepository).findCacheGroupByName(mockitoEq("group1"))(anyObject[Span])
    doReturn(Future.successful(Unit)).when(mockCacheOps).increasePartVersion(anyString())(anyObject[Span])
    whenReady(controller.invalidateCacheGroupParts("group1")(FakeRequest())) { result =>
      verify(mockCacheOps).increasePartVersion(mockHttpPartConfig.partId)
      verify(mockCacheOps).increasePartVersion("another")
      verifyNoMoreInteractions(mockCacheOps)
      result.header.status should be(OK)
    }
  }

  it should "return 404 when /invalidate/cacheGroup/:cacheGroupName/params/:pvalue is called with a non existent cacheGroupName" in {
    val mockCacheOps = mock[CacheOps](RETURNS_SMART_NULLS)
    val mockRepository = mock[ConfigsRepository](RETURNS_SMART_NULLS)
    val controller = new CacheController(mockCacheOps, mockRepository, appComponents.controllerComponents)
    doReturn(Future.successful(None)).when(mockRepository).findCacheGroupByName(mockitoEq("wutthewut"))(anyObject[Span])
    doReturn(Future.successful(Unit)).when(mockCacheOps).increasePartVersion(anyString())(anyObject[Span])
    whenReady(controller.invalidateCacheGroupParam("wutthewut", "irrelevant")(FakeRequest())) { result =>
      result.header.status should be(NOT_FOUND)
      verifyNoMoreInteractions(mockCacheOps)
    }
  }

  it should "return 500 along with a proper string describing the error when a cache invalidation fails" in {
    val mockCacheOps = mock[CacheOps](RETURNS_SMART_NULLS)
    val mockRepository = mock[ConfigsRepository](RETURNS_SMART_NULLS)
    val controller = new CacheController(mockCacheOps, mockRepository, appComponents.controllerComponents)
    doReturn(Future.successful(Some(partCacheGroup))).when(mockRepository).findCacheGroupByName(mockitoEq("group1"))(anyObject[Span])
    doReturn(Future.failed(new IllegalArgumentException("within test"))).when(mockCacheOps).increasePartVersion(anyString())(anyObject[Span])
    val fResult = controller.invalidateCacheGroupParts("group1")(FakeRequest())
    whenReady(fResult) { result =>
      result.header.status should be(INTERNAL_SERVER_ERROR)
      contentAsString(fResult) should include("ERROR")
      verify(mockCacheOps).increasePartVersion(mockHttpPartConfig.partId)
      verify(mockCacheOps).increasePartVersion("another")
      verifyNoMoreInteractions(mockCacheOps)
    }
  }

  it should "return 200 when /invalidate/cacheGroup/:cacheGroupName/params/:pvalue is called with an existing cacheGroupName" in {
    val mockCacheOps = mock[CacheOps](RETURNS_SMART_NULLS)
    val mockRepository = mock[ConfigsRepository](RETURNS_SMART_NULLS)
    val controller = new CacheController(mockCacheOps, mockRepository, appComponents.controllerComponents)
    doReturn(Future.successful(Some(paramCacheGroup))).when(mockRepository).findCacheGroupByName(mockitoEq("group2"))(anyObject[Span])
    doReturn(Future.successful(Unit)).when(mockCacheOps).increaseParamVersion(anyObject[VersionedParamKey]())(anyObject[Span])
    whenReady(controller.invalidateCacheGroupParam("group2", "12345").apply(FakeRequest())) { result =>
      verify(mockCacheOps).increaseParamVersion(VersionedParamKey(mockHttpPartConfig.partId, mockPartParam.outputName, "12345"))
      verify(mockCacheOps).increaseParamVersion(VersionedParamKey("another", "another param", "12345"))
      verifyNoMoreInteractions(mockCacheOps)
      result.header.status should be(OK)
    }
  }
}
