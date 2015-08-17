package controllers

import java.util.UUID

import com.beachape.zipkin.services.{ NoopZipkinService, ZipkinServiceLike }
import com.m3.octoparts.aggregator.PartRequestInfo
import com.m3.octoparts.json.format.ReqResp._
import com.m3.octoparts.aggregator.handler._
import com.m3.octoparts.aggregator.service._
import com.m3.octoparts.model._
import com.m3.octoparts.model.config.HttpPartConfig
import com.m3.octoparts.support.mocks.{ ConfigDataMocks, MockConfigRepository }
import com.twitter.zipkin.gen.Span
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ FlatSpec, Matchers }
import org.scalatestplus.play.OneAppPerSuite
import play.api.libs.json._
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.collection.SortedSet
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class PartsControllerSpec extends FlatSpec with Matchers with MockitoSugar with ConfigDataMocks with OneAppPerSuite {

  import scala.concurrent.ExecutionContext.Implicits.global
  private implicit val emptySpan = new Span()

  private def createConfig(partId: String): HttpPartConfig = mockHttpPartConfig.copy(
    partId = partId,
    uriToInterpolate = "http://www.example.com/" + partId,
    hystrixConfig = Some(mockHystrixConfig)
  )

  private val configsRepository = new MockConfigRepository {
    def keys = Seq("void", "error", "slow")

    override def findConfigByPartId(partId: String)(implicit parentSpan: Span): Future[Option[HttpPartConfig]] = Future.successful {
      if (keys.contains(partId)) Some(createConfig(partId)) else None
    }

    override def findAllConfigs()(implicit parentSpan: Span): Future[SortedSet[HttpPartConfig]] = Future.successful(keys.map(createConfig).to[SortedSet])
  }
  private val voidHandler = new Handler {
    val partId = "something"

    def process(pri: PartRequestInfo, args: HandlerArguments)(implicit parentSpan: Span) = Future.successful(PartResponse(partId, partId))
  }
  private val partsRequestService = new PartRequestService(configsRepository, new HttpHandlerFactory {
    implicit val zipkinService: ZipkinServiceLike = NoopZipkinService
    override def makeHandler(ci: HttpPartConfig) = ci.partId match {
      case "void" => voidHandler
      case _ => throw new RuntimeException
    }
  }, NoopZipkinService)
  private val partsService = new PartsService(partsRequestService)

  private val controller = new PartsController(partsService, configsRepository, 10 seconds, true, NoopZipkinService)

  it should "return 400 to an unknown json" in {
    val json = Json.parse("""{"json":"unknown"}""")
    val result = controller.retrieveParts(FakeRequest().withBody(json))
    status(result) should be(400)
    contentAsString(result) should include("Unrecognized request object")
  }

  it should "return a valid response to a valid request" in {
    val rm = new RequestMeta(UUID.randomUUID.toString)
    val voidName = "void"
    val voidPart = new PartRequest(voidName)
    val ar = new AggregateRequest(rm, List(voidPart))
    val postData = Json.toJson(ar)
    val result = controller.retrieveParts(FakeRequest().withBody(postData))

    status(result) should be(200)
    contentAsJson(result).validate[AggregateResponse].isSuccess shouldBe true
  }

  it should "show a list of octoparts" in {
    val result = controller.list().apply(FakeRequest())

    status(result) should be(200)
    contentAsString(result) should include("void")
    contentAsString(result) should include("error")
    contentAsString(result) should include("slow")
  }

  it should "show a filtered list of octoparts" in {
    val result = controller.list(List("void")).apply(FakeRequest())

    status(result) should be(200)
    contentAsString(result) should include("void")
    contentAsString(result) shouldNot include("error")
    contentAsString(result) shouldNot include("slow")
  }

  it should "show a filtered list of octoparts when POST is used" in {
    val result = controller.listPost.apply(FakeRequest().withJsonBody(Json.obj("ids" -> Json.arr("void"))))

    status(result) should be(200)
    contentAsString(result) should include("void")
    contentAsString(result) shouldNot include("error")
    contentAsString(result) shouldNot include("slow")
  }

}