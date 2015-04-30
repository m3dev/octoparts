package controllers.system

import java.io.IOException

import com.m3.octoparts.cache.RawCache
import com.m3.octoparts.hystrix.HystrixHealthReporter
import com.m3.octoparts.model.config.HttpPartConfig
import com.m3.octoparts.repository.ConfigsRepository
import com.m3.octoparts.support.mocks.ConfigDataMocks
import com.twitter.zipkin.gen.Span
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfterEach, FlatSpec, Matchers }
import org.scalatestplus.play.OneAppPerSuite
import play.api.libs.json.JsValue
import play.api.test.FakeRequest
import play.api.test.Helpers._
import shade.memcached.Codec
import scala.collection.SortedSet
import scala.concurrent.Future
import org.mockito.Matchers._
import org.mockito.Matchers.{ eq => mockitoEq }

class HealthcheckControllerSpec
    extends FlatSpec
    with Matchers
    with MockitoSugar
    with BeforeAndAfterEach
    with JsonCheckSupport
    with OneAppPerSuite
    with ConfigDataMocks {

  private implicit val emptySpan = new Span()
  private val configsRepo = mock[ConfigsRepository](RETURNS_SMART_NULLS)
  private val hystrixHealthReporter = mock[HystrixHealthReporter](RETURNS_SMART_NULLS)
  private val cache = mock[RawCache](RETURNS_SMART_NULLS)

  override def beforeEach() = {
    reset(configsRepo, hystrixHealthReporter, cache)

    // Return healthy-looking results by default
    val mockConfigs = SortedSet(mockHttpPartConfig, mockHttpPartConfig.copy(partId = "another"), mockHttpPartConfig.copy(partId = "yet another"))
    doReturn(Future.successful(mockConfigs)).when(configsRepo).findAllConfigs()(anyObject[Span])
    when(hystrixHealthReporter.getCommandKeysWithOpenCircuitBreakers).thenReturn(Nil)
    when(cache.get[String](mockitoEq("ping"))(anyObject[Codec[String]], anyObject[Span])).thenReturn(Future.successful(Some("pong")))
  }

  {
    val controller = new HealthcheckController(configsRepo, hystrixHealthReporter, cache, SingleMemcachedCacheKeyToCheck)

    it should "return a 200 OK response" in {
      status(controller.healthcheck.apply(FakeRequest())) should be(200)
    }

    it should "be GREEN if DB is healthy, there are no open circuits and Memcached is healthy" in {
      checkJson(controller.healthcheck.apply(FakeRequest())) { implicit json =>
        colour should be("GREEN")
        dbOk shouldBe true
        hystrixOk shouldBe true
        cacheOk shouldBe true
      }
    }

    it should "be YELLOW and show DB as not OK if there are no part configs" in {
      when(configsRepo.findAllConfigs()(anyObject[Span])).thenReturn(Future.successful(SortedSet.empty[HttpPartConfig]))

      checkJson(controller.healthcheck.apply(FakeRequest())) { implicit json =>
        colour should be("YELLOW")
        dbOk shouldBe false
      }
    }

    it should "be YELLOW and show DB as not OK if DB query throws an exception" in {
      when(configsRepo.findAllConfigs()(anyObject[Span])).thenReturn(Future.failed(new RuntimeException("OH MY GOD!")))

      checkJson(controller.healthcheck.apply(FakeRequest())) { implicit json =>
        colour should be("YELLOW")
        dbOk shouldBe false
        (json \ "statuses" \ "db" \ "message").as[String] should include("OH MY GOD!")
      }
    }

    it should "be GREEN and show Hystrix as not OK if there are any open Hystrix circuits" in {
      when(hystrixHealthReporter.getCommandKeysWithOpenCircuitBreakers).thenReturn(Seq("mrkun", "career"))

      checkJson(controller.healthcheck.apply(FakeRequest())) { implicit json =>
        // https://github.com/m3dev/octoparts/pull/150
        colour should be("GREEN")
        hystrixOk shouldBe false
      }
    }

    it should "be YELLOW and show Memcached as not OK if the Memcached GET returned a failure" in {
      when(cache.get[String](mockitoEq("ping"))(anyObject[Codec[String]], anyObject[Span]))
        .thenReturn(Future.failed(new IOException("Memcached is down!")))

      checkJson(controller.healthcheck.apply(FakeRequest())) { implicit json =>
        colour should be("YELLOW")
        cacheOk shouldBe false
      }
    }

    {
      val controller = new HealthcheckController(configsRepo, hystrixHealthReporter, cache, new MemcachedCacheKeysToCheck {
        def apply() = Seq("1", "2", "3")
      })

      it should "be GREEN if DB is healthy, there are no open circuits and all Memcached checks succeeded" in {
        when(cache.get[String](mockitoEq("1"))(anyObject[Codec[String]], anyObject[Span])).thenReturn(Future.successful(None))
        when(cache.get[String](mockitoEq("2"))(anyObject[Codec[String]], anyObject[Span])).thenReturn(Future.successful(None))
        when(cache.get[String](mockitoEq("3"))(anyObject[Codec[String]], anyObject[Span])).thenReturn(Future.successful(None))

        checkJson(controller.healthcheck.apply(FakeRequest())) { implicit json =>
          colour should be("GREEN")
          cacheOk shouldBe true
        }
      }

      it should "be YELLOW and show Memcached as not OK when at least one Memcached GET returns a failure" in {
        when(cache.get[String](mockitoEq("1"))(anyObject[Codec[String]], anyObject[Span])).thenReturn(Future.successful(None))
        when(cache.get[String](mockitoEq("2"))(anyObject[Codec[String]], anyObject[Span])).thenReturn(Future.failed(new IOException("Memcached is down!")))
        when(cache.get[String](mockitoEq("3"))(anyObject[Codec[String]], anyObject[Span])).thenReturn(Future.successful(None))

        checkJson(controller.healthcheck.apply(FakeRequest())) { implicit json =>
          colour should be("YELLOW")
          cacheOk shouldBe false
        }
      }
    }
  }

  def colour(implicit json: JsValue) = (json \ "colour").as[String]

  def dbOk(implicit json: JsValue) = (json \ "statuses" \ "db" \ "ok").as[Boolean]

  def hystrixOk(implicit json: JsValue) = (json \ "statuses" \ "hystrix" \ "ok").as[Boolean]

  def cacheOk(implicit json: JsValue) = (json \ "statuses" \ "memcached" \ "ok").as[Boolean]
}
