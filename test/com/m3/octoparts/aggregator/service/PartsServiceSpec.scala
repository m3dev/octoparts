package com.m3.octoparts.aggregator.service

import org.scalatest.{ Matchers, FlatSpec }
import org.scalatest.mock.MockitoSugar
import com.m3.octoparts.logging.PartRequestLogger
import com.m3.octoparts.model._
import org.mockito.Mockito._
import org.mockito.{ Matchers => MockMatchers }
import scala.concurrent.{ TimeoutException, Future }
import com.m3.octoparts.aggregator.PartRequestInfo
import scala.concurrent.duration._
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import org.scalatest.time.{ Second, Span }
import com.twitter.zipkin.gen.{ Span => TwitterSpan }
import java.io.IOException
import com.m3.octoparts.model.PartResponse

class PartsServiceSpec extends FlatSpec with Matchers with MockitoSugar with ScalaFutures with Eventually {
  import scala.concurrent.ExecutionContext.Implicits.global

  // Because logging (e.g. the WARN when the Future times out) is super-slow (only when writing to sbt console?)
  override implicit val patienceConfig = PatienceConfig(timeout = Span(1, Second))

  behavior of "#processParts"

  val serviceId = Some("myService")
  val partId = "bande a part"
  implicit val emtpySpan = new TwitterSpan()

  it should "log a successful response that was a cache hit" in {
    val partReqService = mock[PartRequestServiceBase]
    val logger = mock[PartRequestLogger]
    val partsService = new PartsService(partReqService, logger)

    val aggReq = AggregateRequest(RequestMeta(id = "foo", serviceId = serviceId), Seq(PartRequest(partId)))
    when(partReqService.responseFor(MockMatchers.any[PartRequestInfo])(MockMatchers.any[TwitterSpan]))
      .thenReturn(Future.successful(PartResponse(partId, partId, retrievedFromCache = true)))

    whenReady(partsService.processParts(aggReq)) { _ =>
      eventually {
        verify(logger, times(1)).logSuccess(MockMatchers.eq(partId), MockMatchers.eq("foo"), MockMatchers.eq(serviceId), MockMatchers.eq(true), MockMatchers.anyLong())
      }
    }
  }

  it should "log a successful response that was a cache miss" in {
    val partReqService = mock[PartRequestServiceBase]
    val logger = mock[PartRequestLogger]
    val partsService = new PartsService(partReqService, logger)

    val aggReq = AggregateRequest(RequestMeta(id = "foo", serviceId = serviceId), Seq(PartRequest(partId)))
    when(partReqService.responseFor(MockMatchers.any[PartRequestInfo])(MockMatchers.any[TwitterSpan]))
      .thenReturn(Future.successful(PartResponse(partId, partId, retrievedFromCache = false)))

    whenReady(partsService.processParts(aggReq)) { _ =>
      eventually {
        verify(logger, times(1)).logSuccess(MockMatchers.eq(partId), MockMatchers.eq("foo"), MockMatchers.eq(serviceId), MockMatchers.eq(false), MockMatchers.anyLong())
      }
    }
  }

  it should "log a response that took longer than the client-specified timeout to respond" in {
    val partReqService = mock[PartRequestServiceBase]
    val logger = mock[PartRequestLogger]
    val partsService = new PartsService(partReqService, logger)

    val aggReq = AggregateRequest(RequestMeta(id = "foo", serviceId = serviceId, timeout = Some(123.millis)), Seq(PartRequest(partId)))
    when(partReqService.responseFor(MockMatchers.any[PartRequestInfo])(MockMatchers.any[TwitterSpan]))
      .thenReturn(Future.failed(new TimeoutException))

    whenReady(partsService.processParts(aggReq)) { _ =>
      verify(logger, times(1)).logTimeout(partId, "foo", serviceId, 123)
    }
  }

  it should "log a response that failed by throwing an exception" in {
    val partReqService = mock[PartRequestServiceBase]
    val logger = mock[PartRequestLogger]
    val partsService = new PartsService(partReqService, logger)

    val aggReq = AggregateRequest(RequestMeta(id = "foo", serviceId = serviceId, timeout = Some(123.millis)), Seq(PartRequest(partId)))
    when(partReqService.responseFor(MockMatchers.any[PartRequestInfo])(MockMatchers.any[TwitterSpan]))
      .thenReturn(Future.failed(new IOException("Oh no!")))

    whenReady(partsService.processParts(aggReq)) { _ =>
      verify(logger, times(1)).logFailure(partId, "foo", serviceId, None)
    }
  }

  it should "log a response with a non-empty errors list" in {
    val partReqService = mock[PartRequestServiceBase]
    val logger = mock[PartRequestLogger]
    val partsService = new PartsService(partReqService, logger)

    val aggReq = AggregateRequest(RequestMeta(id = "foo", serviceId = serviceId), Seq(PartRequest(partId)))
    when(partReqService.responseFor(MockMatchers.any[PartRequestInfo])(MockMatchers.any[TwitterSpan]))
      .thenReturn(Future.successful(PartResponse(partId, partId, statusCode = Some(400), errors = Seq("Bad request!"))))

    whenReady(partsService.processParts(aggReq)) { _ =>
      eventually {
        verify(logger, times(1)).logFailure(partId, "foo", serviceId, Some(400))
      }
    }
  }

}
