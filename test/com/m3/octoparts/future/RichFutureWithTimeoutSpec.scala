package com.m3.octoparts.future

import java.io.IOException
import java.util.concurrent.{ Executors, TimeoutException }

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.m3.octoparts.future.RichFutureWithTimeout._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ FunSpec, Matchers }

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps
import scala.util.Random

class RichFutureWithTimeoutSpec extends FunSpec with Matchers with ScalaFutures {

  implicit val testEC = {
    val namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("future-timeout-%d").build()
    ExecutionContext.fromExecutor(Executors.newCachedThreadPool(namedThreadFactory))
  }

  // Implicit PatienceConfig for ScalaFutures
  implicit val p = PatienceConfig(timeout = 10 seconds)

  describe("#timeoutIn") {

    val fTimeout = 500 millis

    it("should cause a Future to timeout after the passed in duration time") {
      val fWithTimeout: Future[Int] = Future {
        Thread.sleep((fTimeout + (20 millis)).toMillis)
        99
      }.timeoutIn(fTimeout)
      whenReady(fWithTimeout.failed) { e =>
        e shouldBe a[TimeoutException]
        e.getMessage should include(fTimeout.toString())
      }
    }

    it("should cause all Futures to timeout after the passed in duration time") {
      // Playing with n in Seq.fill(n), e.g. increasing it beyond ~300, causes timeout errors in the later tests
      val futuresTimeoutDoubles = Seq.fill(500) {
        val timeout = (300 + Random.nextInt(10)).millis
        val f = Future {
          val start = System.currentTimeMillis()
          // Depending on timeout duration, e.g. if the amount added to the sleep is < 20 millis, the future won't timeout properly
          Thread.sleep((timeout + (20 millis)).toMillis)
          System.currentTimeMillis() - start
        }.timeoutIn(timeout)
        (f, timeout)
      }
      futuresTimeoutDoubles.foreach { fAndTimeout =>
        val (f, timeout) = fAndTimeout
        try (whenReady(f.failed) { e =>
          e shouldBe a[TimeoutException]
          e.getMessage should include(timeout.toString())
        }) catch {
          case _ => {
            whenReady(f) { runtime => fail(s"timeout was $timeout but the future completed in $runtime millis") }
          }
        }
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
