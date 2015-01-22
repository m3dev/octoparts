package controllers

import com.m3.octoparts.cache.CacheOps
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ FlatSpec, Matchers }
import org.scalatestplus.play.OneAppPerSuite
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future
import scala.language.postfixOps

import com.m3.octoparts.support.mocks.{ MockConfigRespository, ConfigDataMocks }
import com.m3.octoparts.model.config.{ PartParam, CacheGroup, HttpPartConfig }
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import com.m3.octoparts.cache.versioning.VersionedParamKey

class CacheControllerSpec extends FlatSpec with Matchers with MockitoSugar with ConfigDataMocks with ScalaFutures {

  val futureUnit = Future.successful(())
  val mockRepository = new MockConfigRespository {
    val configNames = Seq("part1", "part2")
    val paramIds = Seq(1, 2)
    val cacheGroupKeyNames = Seq("group1")

    override def findCacheGroupByName(name: String): Future[Option[CacheGroup]] = Future.successful(
      if (cacheGroupKeyNames.contains(name))
        Some(mockCacheGroup.copy(
        id = Some(42),
        httpPartConfigs = Seq(mockHttpPartConfig, mockHttpPartConfig.copy(partId = "another")),
        partParams = Seq(mockPartParam, mockPartParam)
      ))
      else
        None
    )
    override def findConfigByPartId(partId: String): Future[Option[HttpPartConfig]] = Future.successful {
      if (configNames.contains(partId)) Some(mockHttpPartConfig.copy(id = Some(3), partId = partId)) else None
    }
    override def findParamById(id: Long): Future[Option[PartParam]] = Future.successful {
      Some(mockPartParam.copy(httpPartConfig = Some(mockHttpPartConfig)))
    }
    override def findAllConfigs(): Future[Seq[HttpPartConfig]] = Future.successful(configNames.map { key =>
      mockHttpPartConfig.copy(id = Some(3), partId = key)
    })
  }
  val mockCacheOps = mock[CacheOps]
  val controller = new CacheController(mockCacheOps, mockRepository)

  it should "return 200 and call increasePartVersion with the partId when /invalidate/:partId is called" in {
    doReturn(futureUnit).when(mockCacheOps).increasePartVersion(anyString())
    whenReady(controller.invalidatePart("part1").apply(FakeRequest())) { result =>
      verify(mockCacheOps).increasePartVersion("part1")
      result.header.status should be(200)
    }
  }

  it should
    """
      |return 200 and call increaseParamVersion with the partId, param name, and param value when
      |/invalidate/:partId/:paramName/:paramValue is called""".stripMargin in {
      doReturn(futureUnit).when(mockCacheOps).increaseParamVersion(anyObject[VersionedParamKey]())
      whenReady(controller.invalidatePartParam("part1", "paramName", "paramValue").apply(FakeRequest())) { result =>
        verify(mockCacheOps).increaseParamVersion(VersionedParamKey("part1", "paramName", "paramValue"))
        result.header.status should be(200)
      }
    }

  it should "return 404 when /invalidate/cacheGroup/:cacheGroupName is called with a non existent cacheGroupName" in {
    status(controller.invalidateCacheGroupParts("wutthewut").apply(FakeRequest())) should be(404)
  }

  it should "return 200 when /invalidate/cacheGroup/:cacheGroupName is called with an existing cacheGroupName" in {
    doReturn(futureUnit).when(mockCacheOps).increasePartVersion(anyString())
    whenReady(controller.invalidateCacheGroupParts("group1").apply(FakeRequest())) { result =>
      verify(mockCacheOps).increasePartVersion(mockHttpPartConfig.partId)
      verify(mockCacheOps).increasePartVersion("another")
      result.header.status should be(200)
    }
  }

  it should "return 404 when /invalidate/cacheGroup/:cacheGroupName/params/:pvalue is called with a non existent cacheGroupName" in {
    doReturn(futureUnit).when(mockCacheOps).increasePartVersion(anyString())
    status(controller.invalidateCacheGroupParam("wutthewut", "irrelevant").apply(FakeRequest())) should be(404)
  }

  it should "return 500 along with a proper string describing the error when a cache invalidation fails" in {
    doReturn(Future.failed(new IllegalArgumentException)).when(mockCacheOps).increasePartVersion(anyString())
    (1 until 50).foreach { _ =>
      val result = controller.invalidateCacheGroupParts("group1").apply(FakeRequest())
      status(result) should be(500)
      contentAsString(result) should include("ERROR")
    }
  }

  it should "return 200 and when /invalidate/cacheGroup/:cacheGroupName/params/:pvalue is called with an existing cacheGroupName" in {
    doReturn(futureUnit).when(mockCacheOps).increaseParamVersion(anyObject[VersionedParamKey]())
    whenReady(controller.invalidateCacheGroupParam("group1", "12345").apply(FakeRequest())) { result =>
      /*
       * Even though the CacheGroup is mocked to have 2 different PartParams, the mock repository's
       * findParamById query that re-retrieves each PartParam (in order to eager-load their parent
       * HttpPartConfig) returns the same PartParam all the time, which causes the same arguments to
       * be passed to increaseParamVersion
      */
      verify(mockCacheOps, times(2)).increaseParamVersion(VersionedParamKey(mockHttpPartConfig.partId, mockPartParam.outputName, "12345"))
      result.header.status should be(200)
    }
  }

}

