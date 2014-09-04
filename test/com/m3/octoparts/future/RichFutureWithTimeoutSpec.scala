package com.m3.octoparts.future

import java.io.IOException
import java.util.concurrent.{ CountDownLatch, TimeoutException }

import com.m3.octoparts.future.RichFutureWithTimeout._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ FunSpec, Matchers }
import org.scalatestplus.play.OneAppPerSuite

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.language.postfixOps

class RichFutureWithTimeoutSpec extends FunSpec with Matchers with ScalaFutures {

  import scala.concurrent.ExecutionContext.Implicits.global

  // Implicit PatienceConfig for ScalaFutures
  implicit val p = PatienceConfig(timeout = 10 seconds)

  describe("#timeoutIn") {

    val fTimeout = 500 millis

    it("should cause a Future to timeout after the passed in duration time") {
      val fWithTimeout: Future[Int] = Future {
        Thread.sleep((fTimeout + (100 millis)).toMillis)
        99
      }.timeoutIn(fTimeout)
      whenReady(fWithTimeout.failed) { e =>
        e shouldBe a[TimeoutException]
        e.getMessage should include(fTimeout.toString())
      }
    }

    it("should allow the original future to continue computing even if the new future fails") {
      val futureWithOutTimeout: Future[Int] = Future {
        Thread.sleep((fTimeout + (100 millis)).toMillis)
        99
      }
      val fWithTimeout: Future[Int] = futureWithOutTimeout.timeoutIn(fTimeout)
      whenReady(fWithTimeout.failed)(_ shouldBe a[TimeoutException])
      whenReady(futureWithOutTimeout)(_ should be(99))
    }

    it("should not cause a Future to be timed out before the allotted timeout duration") {
      val fWithTimeout: Future[Int] = Future {
        Thread.sleep((fTimeout - (100 millis)).toMillis)
        99
      } timeoutIn (fTimeout + (100 millis))
      whenReady(fWithTimeout)(_ should be(99))
    }

    it("should fail with the original exception if the original Future fails") {
      val exception = new IOException("welp!")
      val fWithTimeout: Future[Int] = Future[Int] {
        throw exception
      } timeoutIn fTimeout
      whenReady(fWithTimeout.failed)(_ should be(exception))
    }

  }
}
