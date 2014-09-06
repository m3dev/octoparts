package controllers

import com.m3.octoparts.JsonFormats
import com.m3.octoparts.aggregator.handler._
import com.m3.octoparts.aggregator.service._
import com.m3.octoparts.config._
import com.m3.octoparts.model._
import com.m3.octoparts.model.config.HttpPartConfig
import com.m3.octoparts.support.mocks.{ ConfigDataMocks, MockConfigRespository }
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ FlatSpec, Matchers }
import play.api.libs.json.{ JsSuccess, Json }
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class PartsControllerSpec extends FlatSpec with Matchers with MockitoSugar with ConfigDataMocks {

  import JsonFormats._
  import scala.concurrent.ExecutionContext.Implicits.global

  def createConfig(partId: String): HttpPartConfig = mockHttpPartConfig.copy(
    partId = partId,
    uriToInterpolate = "http://www.example.com/" + partId,
    hystrixConfig = Some(mockHystrixConfig)
  )

  val configsRepository = new MockConfigRespository {
    def keys = Seq("void", "error", "slow")

    override def findConfigByPartId(partId: String): Future[Option[HttpPartConfig]] = Future.successful {
      if (keys.contains(partId)) Some(createConfig(partId)) else None
    }

    override def findAllConfigs(): Future[Seq[HttpPartConfig]] = Future.successful(keys.map(createConfig))
  }
  val voidHandler = new Handler {
    val partId = "something"

    def process(args: HandlerArguments) = Future.successful(PartResponse(partId, partId))
  }
  val partsRequestService = new PartRequestService(configsRepository, new HttpHandlerFactory {
    override def makeHandler(ci: HttpPartConfig) = ci.partId match {
      case "void" => voidHandler
      case _ => throw new RuntimeException
    }
  })
  val partsService = new PartsService(partsRequestService)

  val controller = new PartsController(partsService, configsRepository, 10 seconds, true)

  it should "return 400 to an unknown json" in {
    val json = Json.parse("""{"json":"unknown"}""")
    val result = controller.retrieveParts(FakeRequest().withBody(json))
    status(result) should be(400)
    contentAsString(result) should include("Unrecognized request object")
  }

  it should "return a valid response to a valid request" in {
    val rm = new RequestMeta(newId)
    val voidName = "void"
    val voidPart = new PartRequest(voidName)
    val ar = new AggregateRequest(rm, List(voidPart))
    val postData = Json.toJson(ar)
    val result = controller.retrieveParts(FakeRequest().withBody(postData))

    status(result) should be(200)
    contentAsJson(result).validate[AggregateResponse] shouldBe a[JsSuccess[_]]
  }

  it should "show a list of octoparts" in {
    val result = controller.list.apply(FakeRequest())

    status(result) should be(200)
    contentAsString(result) should include("void")
    contentAsString(result) should include("error")
    contentAsString(result) should include("slow")
  }

}