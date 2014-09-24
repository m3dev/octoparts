package com.m3.octoparts.logging

import org.scalatest.{ Matchers, FunSpec }
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import play.api.LoggerLike

class LTSVLogWriterSpec extends FunSpec with MockitoSugar with Matchers {

  val exception = new IllegalArgumentException

  def writerWithMock: (LTSVLogWriter, LoggerLike) = {
    val loggerLike = mock[LoggerLike]
    val writer = new LTSVLogWriter {
      val logger: LoggerLike = loggerLike
    }
    (writer, loggerLike)
  }

  describe("#debug and not passing a Throwable") {
    it("should call .debug on the underlying LoggerLike") {
      val (subject, underlying) = writerWithMock
      subject.debug("hi" -> "there")
      verify(underlying, times(1)).debug(anyString())
    }
  }

  describe("#debug and passing a Throwable") {
    it("should call .debug on the underlying LoggerLike") {
      val (subject, underlying) = writerWithMock
      subject.debug(exception, "hello" -> "there")
      verify(underlying, times(1)).debug(anyString(), anyObject[Throwable])
    }
  }

  describe("#info and not passing a Throwable") {
    it("should call .info on the underlying LoggerLike") {
      val (subject, underlying) = writerWithMock
      subject.info("hi" -> "there")
      verify(underlying, times(1)).info(anyString())
    }
  }

  describe("#info and passing a Throwable") {
    it("should call .info on the underlying LoggerLike") {
      val (subject, underlying) = writerWithMock
      subject.info(exception, "hello" -> "there")
      verify(underlying, times(1)).info(anyString(), anyObject[Throwable])
    }
  }

  describe("#warn and not passing a Throwable") {
    it("should call .warn on the underlying LoggerLike") {
      val (subject, underlying) = writerWithMock
      subject.warn("hi" -> "there")
      verify(underlying, times(1)).warn(anyString())
    }
  }

  describe("#warn and passing a Throwable") {
    it("should call .warn on the underlying LoggerLike") {
      val (subject, underlying) = writerWithMock
      subject.warn(exception, "hello" -> "there")
      verify(underlying, times(1)).warn(anyString(), anyObject[Throwable])
    }
  }

  describe("#error and not passing a Throwable") {
    it("should call .error on the underlying LoggerLike") {
      val (subject, underlying) = writerWithMock
      subject.error("hi" -> "there")
      verify(underlying, times(1)).error(anyString())
    }
  }

  describe("#error and passing a Throwable") {
    it("should call .error on the underlying LoggerLike") {
      val (subject, underlying) = writerWithMock
      subject.error(exception, "hello" -> "there")
      verify(underlying, times(1)).error(anyString(), anyObject[Throwable])
    }
  }

  describe("#trace and not passing a Throwable") {
    it("should call .trace on the underlying LoggerLike") {
      val (subject, underlying) = writerWithMock
      subject.trace("hi" -> "there")
      verify(underlying, times(1)).trace(anyString())
    }
  }

  describe("#trace and passing a Throwable") {
    it("should call .trace on the underlying LoggerLike") {
      val (subject, underlying) = writerWithMock
      subject.trace(exception, "hello" -> "there")
      verify(underlying, times(1)).trace(anyString(), anyObject[Throwable])
    }
  }

}
