package com.m3.octoparts.logging

import org.scalatest.{ Matchers, FunSpec }
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.slf4j.{ Marker, Logger }

class LTSVLogWriterSpec extends FunSpec with MockitoSugar with Matchers {

  val exception = new IllegalArgumentException

  describe("logging with hostnames") {
    it("should send a message with 'host' to the underlying logger") {
      val (writer, captureDebugMsg) = writerWithDebugCapture
      writer.debug(true, "hi" -> "there")
      captureDebugMsg() contains ("host") should be(true)
    }
  }

  describe("logging without hostnames") {
    it("should send a message without 'host' to the underlying logger") {
      val (writer, captureDebugMsg) = writerWithDebugCapture
      writer.debug(false, "hi" -> "there")
      captureDebugMsg() contains ("host") should be(false)
    }
  }

  describe("#debug laziness") {
    it("should not do anything, not even evaluate arguments if debug is disabled") {
      var run = false
      lazy val pair = {
        run = true
        ("hi" -> "there")
      }
      val (subject, underlying) = writerWithMock(debugEnabled = false)
      subject.debug(pair)
      run should be(false)
    }
  }

  describe("#debug and not passing a Throwable") {
    it("should call .debug on the underlying LoggerLike") {
      val (subject, underlying) = writerWithMock()
      subject.debug("hi" -> "there")
      verify(underlying, times(1)).debug(anyString())
    }
  }

  describe("#debug and passing a Throwable") {
    it("should call .debug on the underlying LoggerLike") {
      val (subject, underlying) = writerWithMock()
      subject.debug(exception, "hello" -> "there")
      verify(underlying, times(1)).debug(anyString(), anyObject[Throwable])
    }
  }

  describe("#info laziness") {
    it("should not do anything, not even evaluate arguments if info is disabled") {
      var run = false
      lazy val pair = {
        run = true
        ("hi" -> "there")
      }
      val (subject, underlying) = writerWithMock(infoEnabled = false)
      subject.info(pair)
      run should be(false)
    }
  }

  describe("#info and not passing a Throwable") {
    it("should call .info on the underlying LoggerLike") {
      val (subject, underlying) = writerWithMock()
      subject.info("hi" -> "there")
      verify(underlying, times(1)).info(anyString())
    }
  }

  describe("#info and passing a Throwable") {
    it("should call .info on the underlying LoggerLike") {
      val (subject, underlying) = writerWithMock()
      subject.info(exception, "hello" -> "there")
      verify(underlying, times(1)).info(anyString(), anyObject[Throwable])
    }
  }

  describe("#warn laziness") {
    it("should not do anything, not even evaluate arguments if warn is disabled") {
      var run = false
      lazy val pair = {
        run = true
        ("hi" -> "there")
      }
      val (subject, underlying) = writerWithMock(warnEnabled = false)
      subject.warn(pair)
      run should be(false)
    }
  }

  describe("#warn and not passing a Throwable") {
    it("should call .warn on the underlying LoggerLike") {
      val (subject, underlying) = writerWithMock()
      subject.warn("hi" -> "there")
      verify(underlying, times(1)).warn(anyString())
    }
  }

  describe("#warn and passing a Throwable") {
    it("should call .warn on the underlying LoggerLike") {
      val (subject, underlying) = writerWithMock()
      subject.warn(exception, "hello" -> "there")
      verify(underlying, times(1)).warn(anyString(), anyObject[Throwable])
    }
  }

  describe("#error laziness") {
    it("should not do anything, not even evaluate arguments if error is disabled") {
      var run = false
      lazy val pair = {
        run = true
        ("hi" -> "there")
      }
      val (subject, underlying) = writerWithMock(errorEnabled = false)
      subject.error(pair)
      run should be(false)
    }
  }

  describe("#error and not passing a Throwable") {
    it("should call .error on the underlying LoggerLike") {
      val (subject, underlying) = writerWithMock()
      subject.error("hi" -> "there")
      verify(underlying, times(1)).error(anyString())
    }
  }

  describe("#error and passing a Throwable") {
    it("should call .error on the underlying LoggerLike") {
      val (subject, underlying) = writerWithMock()
      subject.error(exception, "hello" -> "there")
      verify(underlying, times(1)).error(anyString(), anyObject[Throwable])
    }
  }

  describe("#trace laziness") {
    it("should not do anything, not even evaluate arguments if trace is disabled") {
      var run = false
      lazy val pair = {
        run = true
        ("hi" -> "there")
      }
      val (subject, underlying) = writerWithMock(traceEnabled = false)
      subject.trace(pair)
      run should be(false)
    }
  }

  describe("#trace and not passing a Throwable") {
    it("should call .trace on the underlying LoggerLike") {
      val (subject, underlying) = writerWithMock()
      subject.trace("hi" -> "there")
      verify(underlying, times(1)).trace(anyString())
    }
  }

  describe("#trace and passing a Throwable") {
    it("should call .trace on the underlying LoggerLike") {
      val (subject, underlying) = writerWithMock()
      subject.trace(exception, "hello" -> "there")
      verify(underlying, times(1)).trace(anyString(), anyObject[Throwable])
    }
  }

  def writerWithMock(debugEnabled: Boolean = true,
                     errorEnabled: Boolean = true,
                     infoEnabled: Boolean = true,
                     traceEnabled: Boolean = true,
                     warnEnabled: Boolean = true): (LTSVLogWriter, Logger) = {
    val loggerLike = mock[Logger]
    when(loggerLike.isDebugEnabled).thenReturn(debugEnabled)
    when(loggerLike.isErrorEnabled).thenReturn(errorEnabled)
    when(loggerLike.isInfoEnabled).thenReturn(infoEnabled)
    when(loggerLike.isTraceEnabled).thenReturn(traceEnabled)
    when(loggerLike.isWarnEnabled).thenReturn(warnEnabled)
    val writer = new LTSVLogWriter {
      val logger: Logger = loggerLike
    }
    (writer, loggerLike)
  }

  def writerWithDebugCapture: (LTSVLogWriter, Function0[String]) = {
    var debugMessage: String = ""
    val loggerLike = new Logger {
      override def debug(message: String): Unit = {
        debugMessage = message
      }

      override def getName: String = ???

      override def warn(msg: String): Unit = ???

      override def warn(format: String, arg: scala.Any): Unit = ???

      override def warn(format: String, arguments: AnyRef*): Unit = ???

      override def warn(format: String, arg1: scala.Any, arg2: scala.Any): Unit = ???

      override def warn(msg: String, t: Throwable): Unit = ???

      override def warn(marker: Marker, msg: String): Unit = ???

      override def warn(marker: Marker, format: String, arg: scala.Any): Unit = ???

      override def warn(marker: Marker, format: String, arg1: scala.Any, arg2: scala.Any): Unit = ???

      override def warn(marker: Marker, format: String, arguments: AnyRef*): Unit = ???

      override def warn(marker: Marker, msg: String, t: Throwable): Unit = ???

      override def isErrorEnabled: Boolean = true

      override def isErrorEnabled(marker: Marker): Boolean = true

      override def isInfoEnabled: Boolean = true

      override def isInfoEnabled(marker: Marker): Boolean = true

      override def isDebugEnabled: Boolean = true

      override def isDebugEnabled(marker: Marker): Boolean = true

      override def isTraceEnabled: Boolean = true

      override def isTraceEnabled(marker: Marker): Boolean = true

      override def error(msg: String): Unit = ???

      override def error(format: String, arg: scala.Any): Unit = ???

      override def error(format: String, arg1: scala.Any, arg2: scala.Any): Unit = ???

      override def error(format: String, arguments: AnyRef*): Unit = ???

      override def error(msg: String, t: Throwable): Unit = ???

      override def error(marker: Marker, msg: String): Unit = ???

      override def error(marker: Marker, format: String, arg: scala.Any): Unit = ???

      override def error(marker: Marker, format: String, arg1: scala.Any, arg2: scala.Any): Unit = ???

      override def error(marker: Marker, format: String, arguments: AnyRef*): Unit = ???

      override def error(marker: Marker, msg: String, t: Throwable): Unit = ???

      override def debug(format: String, arg: scala.Any): Unit = ???

      override def debug(format: String, arg1: scala.Any, arg2: scala.Any): Unit = ???

      override def debug(format: String, arguments: AnyRef*): Unit = ???

      override def debug(msg: String, t: Throwable): Unit = ???

      override def debug(marker: Marker, msg: String): Unit = ???

      override def debug(marker: Marker, format: String, arg: scala.Any): Unit = ???

      override def debug(marker: Marker, format: String, arg1: scala.Any, arg2: scala.Any): Unit = ???

      override def debug(marker: Marker, format: String, arguments: AnyRef*): Unit = ???

      override def debug(marker: Marker, msg: String, t: Throwable): Unit = ???

      override def isWarnEnabled: Boolean = ???

      override def isWarnEnabled(marker: Marker): Boolean = ???

      override def trace(msg: String): Unit = ???

      override def trace(format: String, arg: scala.Any): Unit = ???

      override def trace(format: String, arg1: scala.Any, arg2: scala.Any): Unit = ???

      override def trace(format: String, arguments: AnyRef*): Unit = ???

      override def trace(msg: String, t: Throwable): Unit = ???

      override def trace(marker: Marker, msg: String): Unit = ???

      override def trace(marker: Marker, format: String, arg: scala.Any): Unit = ???

      override def trace(marker: Marker, format: String, arg1: scala.Any, arg2: scala.Any): Unit = ???

      override def trace(marker: Marker, format: String, argArray: AnyRef*): Unit = ???

      override def trace(marker: Marker, msg: String, t: Throwable): Unit = ???

      override def info(msg: String): Unit = ???

      override def info(format: String, arg: scala.Any): Unit = ???

      override def info(format: String, arg1: scala.Any, arg2: scala.Any): Unit = ???

      override def info(format: String, arguments: AnyRef*): Unit = ???

      override def info(msg: String, t: Throwable): Unit = ???

      override def info(marker: Marker, msg: String): Unit = ???

      override def info(marker: Marker, format: String, arg: scala.Any): Unit = ???

      override def info(marker: Marker, format: String, arg1: scala.Any, arg2: scala.Any): Unit = ???

      override def info(marker: Marker, format: String, arguments: AnyRef*): Unit = ???

      override def info(marker: Marker, msg: String, t: Throwable): Unit = ???
    }
    val writer = new LTSVLogWriter {
      val logger: Logger = loggerLike
    }
    (writer, () => debugMessage)
  }

}
