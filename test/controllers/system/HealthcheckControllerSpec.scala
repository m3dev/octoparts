package controllers.system

import org.mockito.Mockito._
import org.scalatest.{ Matchers, FlatSpec, BeforeAndAfterEach }
import org.scalatest.mock.MockitoSugar

import com.m3.octoparts.hystrix.HystrixHealthReporter
import com.m3.octoparts.repository.ConfigsRepository
import com.m3.octoparts.model.config.HttpPartConfig
import org.scalatestplus.play.OneAppPerSuite
import play.api.libs.json.JsValue
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scala.concurrent.Future

class HealthcheckControllerSpec
    extends FlatSpec
    with Matchers
    with MockitoSugar
    with BeforeAndAfterEach
    with JsonCheckSupport
    with OneAppPerSuite {

  val mockConfigs = Seq(mock[HttpPartConfig], mock[HttpPartConfig], mock[HttpPartConfig])
  val configsRepo = mock[ConfigsRepository]
  val hystrixHealthReporter = mock[HystrixHealthReporter]

  override def beforeEach() {
    reset(configsRepo, hystrixHealthReporter)

    // Return healthy-looking results by default
    when(configsRepo.findAllConfigs).thenReturn(Future.successful(mockConfigs))
    when(hystrixHealthReporter.getCommandKeysWithOpenCircuitBreakers).thenReturn(Seq())
  }

  val controller = new HealthcheckController(configsRepo, hystrixHealthReporter)

  it should "return a 200 OK response" in {
    status(controller.healthcheck.apply(FakeRequest())) should be(200)
  }

  it should "be GREEN if DB is healthy and there are no open circuits" in {
    checkJson(controller.healthcheck.apply(FakeRequest())) { implicit json =>
      colour should be("GREEN")
      dbOk should be(true)
      hystrixOk should be(true)
    }
  }

  it should "be YELLOW and show DB as not OK if there are no part configs" in {
    when(configsRepo.findAllConfigs).thenReturn(Future.successful(Seq.empty))

    checkJson(controller.healthcheck.apply(FakeRequest())) { implicit json =>
      colour should be("YELLOW")
      dbOk should be(false)
    }
  }

  it should "be YELLOW and show DB as not OK if DB query throws an exception" in {
    when(configsRepo.findAllConfigs).thenReturn(Future.failed(new RuntimeException("OH MY GOD!")))

    checkJson(controller.healthcheck.apply(FakeRequest())) { implicit json =>
      colour should be("YELLOW")
      dbOk should be(false)
      (json \ "statuses" \ "db" \ "message").as[String] should include("OH MY GOD!")
    }
  }

  it should "be YELLOW and show Hystrix as not OK if there are any open Hystrix circuits" in {
    when(hystrixHealthReporter.getCommandKeysWithOpenCircuitBreakers).thenReturn(Seq("mrkun", "career"))

    checkJson(controller.healthcheck.apply(FakeRequest())) { implicit json =>
      colour should be("YELLOW")
      hystrixOk should be(false)
    }
  }

  def colour(implicit json: JsValue) = (json \ "colour").as[String]
  def dbOk(implicit json: JsValue) = (json \ "statuses" \ "db" \ "ok").as[Boolean]
  def hystrixOk(implicit json: JsValue) = (json \ "statuses" \ "hystrix" \ "ok").as[Boolean]
}
