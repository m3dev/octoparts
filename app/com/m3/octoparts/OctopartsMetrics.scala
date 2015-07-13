package com.m3.octoparts

import java.io.StringWriter

import com.codahale.metrics.{ MetricRegistry, SharedMetricRegistries }
import com.fasterxml.jackson.databind.{ ObjectWriter, ObjectMapper }
import com.kenshoo.play.metrics.Metrics
import play.api.Play

object OctopartsMetrics extends Metrics {

  /**
   * Same as [[com.kenshoo.play.metrics.MetricsRegistry.defaultRegistry]] when a Play app is running; uses the default name ("default") otherwise
   */
  val default: MetricRegistry = SharedMetricRegistries.getOrCreate(Play.maybeApplication.flatMap(_.configuration.getString("metrics.name")).getOrElse("default"))

  val mapper: ObjectMapper = new ObjectMapper()

  def defaultRegistry: MetricRegistry = default

  def toJson: String = {

    val writer: ObjectWriter = mapper.writerWithDefaultPrettyPrinter()
    val stringWriter = new StringWriter()
    writer.writeValue(stringWriter, defaultRegistry)
    stringWriter.toString
  }

}
