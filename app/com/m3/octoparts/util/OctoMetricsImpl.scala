package com.m3.octoparts.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.kenshoo.play.metrics.MetricsImpl
import play.api.{ Logger, Configuration }
import play.api.inject.ApplicationLifecycle

import scala.util.control.NonFatal

/**
 * During tests, we occasionally have cases where the asynchronous shutting down of an application means that
 * the previous registry in the singleton has not been unregistered before starting the next test.
 *
 * This implementation of Metrics overrides onStart to perform onStop cleanups first before proceeding with
 * other onStart duties
 */
class OctoMetricsImpl(
    lifecycle: ApplicationLifecycle,
    configuration: Configuration
) extends MetricsImpl(lifecycle, configuration) {

  override def onStart(): ObjectMapper = {
    try {
      super.onStop()
    } catch {
      case NonFatal(e) => Logger.warn("Preemptive cleanup failed", e)
    }
    super.onStart()
  }
}
