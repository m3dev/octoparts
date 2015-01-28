package com.m3.octoparts.aggregator.service

import com.m3.octoparts.aggregator.PartRequestInfo
import com.m3.octoparts.aggregator.handler.HttpHandlerFactory
import com.m3.octoparts.model.PartResponse
import com.m3.octoparts.model.config.{ HttpPartConfig, ShortPartParam }
import com.m3.octoparts.repository.ConfigsRepository
import org.mockito.Mockito._
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ FlatSpec, Matchers }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ ExecutionContext, Future }

class PartResponseLocalContentSupportSpec extends FlatSpec
    with Matchers with MockitoSugar with ScalaFutures with Eventually {

  behavior of "#processWithConfig"

  private val partResponseFromSuper = mock[PartResponse]

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
    val httpPartConfig = mock[HttpPartConfig]
    when(httpPartConfig.localContentsEnabled).thenReturn(false)

    val partRequestInfo = mock[PartRequestInfo]
    val params = mock[Map[ShortPartParam, Seq[String]]]

    whenReady(sut.processWithConfig(httpPartConfig, partRequestInfo, params)) { resp =>
      resp should be theSameInstanceAs partResponseFromSuper
    }
  }

  it should "return response as local contents if local contents is enabled" in {
    val httpPartConfig = mock[HttpPartConfig]
    when(httpPartConfig.localContentsEnabled).thenReturn(true)
    when(httpPartConfig.localContents).thenReturn(Some("localContents"))
    when(httpPartConfig.partId).thenReturn("hogefuga")

    val partRequestInfo = mock[PartRequestInfo]
    val params = mock[Map[ShortPartParam, Seq[String]]]

    whenReady(sut.processWithConfig(httpPartConfig, partRequestInfo, params)) { resp =>
      resp should not be theSameInstanceAs(partResponseFromSuper)
      resp should be(PartResponse(
        partId = "hogefuga",
        id = "hogefuga",
        statusCode = Some(200),
        contents = Some("localContents"),
        retrievedFromLocalContents = true
      ))
    }
  }
}