package com.m3.octoparts.hystrix

import org.scalatest.{ Matchers, FunSpec }
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.duration._
import scala.language.postfixOps
import java.util.concurrent.TimeoutException
import com.netflix.hystrix.exception.HystrixRuntimeException
import com.m3.octoparts.support.mocks.ConfigDataMocks

class HystrixExecutorSpec extends FunSpec with Matchers with ScalaFutures with ConfigDataMocks {

  val noFallbackConfig = mockHttpPartConfig.copy(
    localContents = Some("9"),
    hystrixConfig = Some(mockHystrixConfig.copy(timeoutInMs = 500L)))

  val fallbackConfig = mockHttpPartConfig.copy(
    localContents = Some("9"),
    hystrixConfig = Some(mockHystrixConfig.copy(localContentsAsFallback = true, timeoutInMs = 500L)))

  implicit val p = PatienceConfig(timeout = 5 seconds)

  describe("#future") {

    it("should return a Future[Result] that makes sense") {
      val executor = HystrixExecutor(noFallbackConfig)
      val f = executor.future(3, _.map(_.toInt).getOrElse(90))
      whenReady(f) { _ should be(3) }
    }

    describe("failure handling") {

      it("should time out if blocked beyond the timeout limit") {
        val executor = HystrixExecutor(noFallbackConfig)
        val f = executor.future(
          Thread.sleep((3 seconds).toMillis),
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
            Thread.sleep((3 seconds).toMillis)
            9
          },
          _.map(_.toInt).getOrElse(90)
        )
        whenReady(f) { _ shouldBe 9 }
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
        whenReady(f) { _ shouldBe 9 }
      }

    }

  }

}
