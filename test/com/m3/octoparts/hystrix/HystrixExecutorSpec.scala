package com.m3.octoparts.hystrix

import org.scalatest.{ Matchers, FunSpec }
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.duration._
import java.util.concurrent.TimeoutException
import com.netflix.hystrix.exception.HystrixRuntimeException
import com.m3.octoparts.support.mocks.ConfigDataMocks

class HystrixExecutorSpec extends FunSpec with Matchers with ScalaFutures with ConfigDataMocks {

  lazy val hystrixArguments = mockHystrixConfig.copy(timeoutInMs = 100.milliseconds)

  implicit val p = PatienceConfig(timeout = 2.seconds)

  describe("#future") {
    it("should return a Future[Result] that makes sense") {
      val executor = HystrixExecutor(hystrixArguments)
      val f = executor.future { 3 }
      whenReady(f) { _ should be(3) }
    }
    it("should time out if blocked beyond the timeout limit") {
      val executor = HystrixExecutor(hystrixArguments)
      val f = executor.future {
        Thread.sleep(3.seconds.toMillis)
      }
      whenReady(f.failed) { e =>
        e shouldBe a[HystrixRuntimeException]
        e.getCause shouldBe a[TimeoutException]
      }
    }
  }

}
