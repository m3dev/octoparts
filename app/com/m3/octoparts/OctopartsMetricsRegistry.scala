package com.m3.octoparts

import com.codahale.metrics.SharedMetricRegistries
import play.api.Play

object OctopartsMetricsRegistry {
  /**
   * Same as [[com.kenshoo.play.metrics.MetricsRegistry.defaultRegistry]] when a Play app is running; uses the default name ("default") otherwise
   */
  val default = SharedMetricRegistries.getOrCreate(Play.maybeApplication.flatMap(_.configuration.getString("metrics.name")).getOrElse("default"))
}
