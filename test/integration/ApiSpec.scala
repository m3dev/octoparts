package integration

import com.m3.octoparts.model.{ PartRequest, RequestMeta, AggregateRequest }
import com.m3.octoparts.support.db.RequiresDB
import com.m3.octoparts.support.{ OneDIBrowserPerSuite, PlayServerSupport }
import com.m3.octoparts.ws.OctoClient
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatest.{ Matchers, FunSpec }
import org.scalatestplus.play.HtmlUnitFactory
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class ApiSpec
    extends FunSpec
    with PlayServerSupport
    with OneDIBrowserPerSuite
    with HtmlUnitFactory
    with Matchers
    with ScalaFutures
    with PagesSupport
    with IntegrationPatience {

  // 30 second wait to allow the server to start up
  lazy val octoClient = new OctoClient(appComponents.wsClient, baseUrl, 30.seconds)

  describe("end-to-end integration testing with the OctoClient") {

    describe("#listEndpoints") {

      it("should work") {
        val fEndpoints = octoClient.listEndpoints()
        whenReady(fEndpoints)(l => l.size should be >= 0)
      }
    }

    describe("#invoke") {

      it("should return the proper contents and status when using a existing working Parts mixed with non existing Parts") {
        val (_, threadPoolConfig) = ThreadPoolAddPage.createThreadPool
        val PartAddPage.PartConfig(partId, _, _, _, _, _) = PartAddPage.createPart(threadPoolConfig = threadPoolConfig, addProxy = false)
        val fEndpoints = octoClient.listEndpoints()
        whenReady(fEndpoints) { l =>
          l.size should be > 0
        } // must be greater than zero at this point

        val bogusPartId = "nope-this-should-not-work"
        val aggReq = AggregateRequest(
          RequestMeta.apply("testId"),
          Seq(
            PartRequest(partId),
            PartRequest("nope-this-should-not-work")
          )
        )
        val fAggResp = octoClient.invoke(aggReq)
        whenReady(fAggResp) { aggResp =>
          val partResp = aggResp.findPart(partId).get // must work
          partResp.statusCode.foreach(_ should be >= 200)
          partResp.contents.foreach(_.length should be > 0)

          val bogusPartResp = aggResp.findPart(bogusPartId).get // should still exist
          bogusPartResp.errors should not be 'empty
          bogusPartResp.errors.find(_.contains("unsupported")) shouldBe 'defined
        }
      }
    }

  }

}