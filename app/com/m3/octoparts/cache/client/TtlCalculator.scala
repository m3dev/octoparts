package com.m3.octoparts.cache.client

import java.util.concurrent.TimeUnit

import com.m3.octoparts.model.CacheControl
import org.joda.time.DateTimeUtils

import scala.concurrent.duration.Duration

trait TtlCalculator {

  /**
   * Calculate the correct amount of time to cache a PartResponse,
   * given the expiresAt field (based on a Cache-Control: max-age header returned from the backend API)
   * and the part's configured TTL.
   */
  def calculateTtl(cacheControl: CacheControl, configuredTtl: Option[Duration]): Option[Duration] = {
    if (cacheControl.canRevalidate) {
      // if there is a revalidation method, expiresAt does not relate to memcached eviction
      configuredTtl
    } else {
      val expiresIn = cacheControl.expiresAt
        .map(e => Duration(e - DateTimeUtils.currentTimeMillis(), TimeUnit.MILLISECONDS) max Duration.Zero) // avoid negative durations

      // Choose the shorter of the 2 TTLs
      Seq(expiresIn, configuredTtl).flatten.reduceLeftOption(_ min _)
    }
  }

}
