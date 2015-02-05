package com.m3.octoparts.aggregator.service

import com.m3.octoparts.aggregator.PartRequestInfo
import com.m3.octoparts.aggregator.handler.HttpHandlerFactory
import com.m3.octoparts.model.PartResponse
import com.m3.octoparts.model.config.{ HttpPartConfig, ShortPartParam }
import com.m3.octoparts.repository.ConfigsRepository
import com.m3.octoparts.support.mocks.ConfigDataMocks
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ FlatSpec, Matchers }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ ExecutionContext, Future }

class PartResponseLocalContentSupportSpec extends FlatSpec
    with Matchers with ScalaFutures with ConfigDataMocks {

  behavior of "#processWithConfig"

  private val partResponseFromSuper = mockPartResponse

  private class Super extends PartRequestServiceBase {
    override implicit def executionContext: ExecutionContext = global
    override def repository: ConfigsRepository = ???
    override def handlerFactory: HttpHandlerFactory = ???
    override def processWithConfig(ci: HttpPartConfig,
                                   partRequestInfo: PartRequestInfo,
                                   params: Map[ShortPartParam, Seq[String]]): Future[PartResponse] = Future(partResponseFromSuper)
  }

  private val sut = new Super with PartResponseLocalContentSupport

  it should "forward to super if local contents is disabled" in {
    val httpPartConfig = mockHttpPartConfig.copy(localContentsEnabled = false)
    val partRequestInfo = mockPartRequestInfo
    val params = Map.empty[ShortPartParam, Seq[String]]

    whenReady(sut.processWithConfig(httpPartConfig, partRequestInfo, params)) { resp =>
      resp should be theSameInstanceAs partResponseFromSuper
    }
  }

  it should "return response as local contents if local contents is enabled" in {
    val httpPartConfig = mockHttpPartConfig
    val partRequestInfo = mockPartRequestInfo
    val params = Map.empty[ShortPartParam, Seq[String]]

    whenReady(sut.processWithConfig(httpPartConfig, partRequestInfo, params)) { resp =>
      resp should not be theSameInstanceAs(partResponseFromSuper)
      resp should be(PartResponse(
        partId = "something",
        id = "id",
        statusCode = Some(200),
        contents = Some("{}"),
        retrievedFromLocalContents = true
      ))
    }
  }
}