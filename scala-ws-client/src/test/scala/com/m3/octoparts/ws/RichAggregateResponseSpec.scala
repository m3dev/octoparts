package com.m3.octoparts.ws

import java.io.IOException

import com.m3.octoparts.ws.AggregateResponseEnrichment._
import com.m3.octoparts.model.{ PartResponse, ResponseMeta, AggregateResponse }
import org.scalatest.{ Matchers, FlatSpec }
import play.api.libs.json.Json
import scala.concurrent.duration._
import scala.util.Success

class RichAggregateResponseSpec extends FlatSpec with Matchers {

  behavior of "#getJsonPart"

  case class Frisk(mints: Int, mintiness: String)
  object Frisk {
    implicit val _ = Json.reads[Frisk]
  }
  case class Error(msg: String)
  object Error {
    implicit val _ = Json.reads[Error]
  }

  val aggResp = AggregateResponse(ResponseMeta("foo", 123.millis), Seq(
    PartResponse("myJsonPart", "myJsonPart", statusCode = Some(200), contents = Some("""{"mints": 50, "mintiness": "very minty"}""")),
    PartResponse("invalidJsonPart", "invalidJsonPart", statusCode = Some(200), contents = Some("""{"error": "something went wrong!"}""")),
    PartResponse("brokenJsonPart", "brokenJsonPart", statusCode = Some(200), contents = Some("""{"mints": }""")),
    PartResponse("noContentPart", "noContentPart", statusCode = Some(200), contents = None),
    PartResponse("errorPart", "errorPart", statusCode = Some(500), contents = Some("""{"mints": 50, "mintiness": "very minty"}"""), errors = Seq("boo1", "boo2")),
    PartResponse("errorPartWithValidError", "errorPartWithValidError", statusCode = Some(400), contents = Some("""{"msg": "bad request"}"""), errors = Seq("That was a bad request.")),
    PartResponse("errorPartWithBrokenError", "errorPartWithBrokenError", statusCode = Some(500), contents = Some("Internal Server Error"), errors = Seq("boo1", "boo2")),
    PartResponse("differentStatusCodePart", "differentStatusCodePart", statusCode = Some(404), contents = Some("""{"mints": 50, "mintiness": "very minty"}"""))
  ))
  val richAggResp = new RichAggregateResponse(aggResp)

  it should "successfully find a PartResponse by partId and deserialize its contents" in {
    richAggResp.tryJsonPart[Frisk]("myJsonPart") should be(Success(Frisk(50, "very minty")))
  }
  it should "find a PartResponse by partId and deserialize its contents" in {
    richAggResp.getJsonPart[Frisk]("myJsonPart") should be(Some(Frisk(50, "very minty")))
  }
  it should "return None if the Json cannot be deserialize to the given type" in {
    richAggResp.getJsonPart[Frisk]("invalidJsonPart") should be(None)
  }
  it should "return None if the Json cannot be deserialized because it is broken" in {
    richAggResp.getJsonPart[Frisk]("brokenJsonPart") should be(None)
  }
  it should "return None if the part has no content" in {
    richAggResp.getJsonPart[Frisk]("noContentPart") should be(None)
  }
  it should "give up on deserialization if the result has error messages" in {
    richAggResp.getJsonPart[Frisk]("errorPart") should be(None)
  }
  it should "deserialize errors if asked nicely" in {
    richAggResp.getJsonPartOrError[Frisk, Error]("errorPartWithValidError") should be(Left(Some(Error("bad request"))))
  }
  it should "fail to deserialize errors even if asked nicely" in {
    richAggResp.getJsonPartOrError[Frisk, Error]("errorPartWithBrokenError") should be(Left(None))
  }
  it should "return None if the part does not exist" in {
    richAggResp.getJsonPart[Frisk]("whoAreYou") should be(None)
  }
  it should "deserialize and return the result even if the status code is not 200" in {
    richAggResp.getJsonPart[Frisk]("differentStatusCodePart") should be(Some(Frisk(50, "very minty")))
  }
}
