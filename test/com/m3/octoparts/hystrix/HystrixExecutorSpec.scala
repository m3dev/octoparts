package com.m3.octoparts.hystrix

import java.util.concurrent.TimeoutException

import com.m3.octoparts.support.mocks.ConfigDataMocks
import com.netflix.hystrix.exception.HystrixRuntimeException
import org.scalatest.concurrent.{ PatienceConfiguration, IntegrationPatience, ScalaFutures }
import org.scalatest.{ FunSpec, Matchers }

import scala.concurrent.duration._

class HystrixExecutorSpec
    extends FunSpec
    with Matchers
    with ScalaFutures
    with ConfigDataMocks
    with IntegrationPatience
    with PatienceConfiguration {

  val noFallbackConfig = mockHttpPartConfig.copy(
    localContents = Some("9"),
    hystrixConfig = Some(mockHystrixConfig.copy(timeout = 5.seconds)))

  val fallbackConfig = mockHttpPartConfig.copy(
    localContents = Some("9"),
    hystrixConfig = Some(mockHystrixConfig.copy(localContentsAsFallback = true, timeout = 5.seconds)))

  implicit val p = PatienceConfig(timeout = 5.seconds)

  describe("#future") {
    it("should return a Future[Result] that makes sense") {
      val executor = HystrixExecutor(noFallbackConfig)
      val f = executor.future(3, _.map(_.toInt).getOrElse(90))
      whenReady(f) {
        _ should be(3)
      }
    }
    describe("failure handling") {
      it("should time out if blocked beyond the timeout limit") {
        val executor = HystrixExecutor(noFallbackConfig)
        val f = executor.future(
          Thread.sleep(3.seconds.toMillis),
          _ => ()
        )
        whenReady(f.failed) { e =>
          e shouldBe a[HystrixRuntimeException]
          e.getCause shouldBe a[TimeoutException]
        }
      }
      it("should not time out if blocked beyond the timeout limit and localContentsAsFallback is true") {
        val executor = HystrixExecutor(fallbackConfig)
        val f = executor.future(
          {
            Thread.sleep(3.seconds.toMillis)
            9
          },
          _.map(_.toInt).getOrElse(90)
        )
        whenReady(f) {
          _ shouldBe 9
        }
      }

      it("should not error out if the function throws and localContentsAsFallback is true") {
        val executor = HystrixExecutor(fallbackConfig)
        val f = executor.future(
          {
            throw new IllegalArgumentException
            9
          },
          _.map(_.toInt).getOrElse(90)
        )
        whenReady(f) {
          _ shouldBe 9
        }
      }

    }
  }

}
