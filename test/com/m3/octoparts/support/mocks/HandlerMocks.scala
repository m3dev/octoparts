package com.m3.octoparts.support.mocks

import com.beachape.zipkin.services.NoopZipkinService
import com.m3.octoparts.aggregator.PartRequestInfo
import com.m3.octoparts.aggregator.handler.{ HttpHandlerFactory, Handler }
import com.twitter.zipkin.gen.Span
import scala.concurrent.Future
import com.m3.octoparts.model.config.HttpPartConfig
import com.m3.octoparts.model.PartResponse

/**
 * Trait that contains useful handler-related mocks
 */
trait HandlerMocks {

  val mockVoidHandler = new Handler {
    val partId = "voidHandler"
    def process(pri: PartRequestInfo, args: HandlerArguments)(implicit span: Span) = Future.successful(PartResponse(partId, partId, contents = Some("it worked")))
  }
  val mockErrorHandler = new Handler {
    val partId = "errorHandler"
    def process(pri: PartRequestInfo, args: HandlerArguments)(implicit span: Span) = Future.failed(new RuntimeException)
  }
  val mockPartResponseWithErrorHandler = new Handler {
    val partId = "errorHandler"
    def process(pri: PartRequestInfo, args: HandlerArguments)(implicit span: Span) = Future.successful(PartResponse(partId, partId, errors = Seq("SomeException")))
  }
  val mockVoidHttpHandlerFactory = new HttpHandlerFactory {
    implicit val zipkinService = NoopZipkinService
    override def makeHandler(ci: HttpPartConfig) = mockVoidHandler
  }
  val mockErrorHttpHandlerFactory = new HttpHandlerFactory {
    implicit val zipkinService = NoopZipkinService
    override def makeHandler(ci: HttpPartConfig) = mockErrorHandler
  }
  val mockPartResponseWithErrorHttpHandlerFactory = new HttpHandlerFactory {
    implicit val zipkinService = NoopZipkinService
    override def makeHandler(ci: HttpPartConfig) = mockPartResponseWithErrorHandler
  }

}
