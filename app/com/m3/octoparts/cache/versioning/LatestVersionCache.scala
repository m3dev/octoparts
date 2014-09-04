package com.m3.octoparts.cache.versioning

import java.util.concurrent.atomic.AtomicLong

import scala.util.Random

object LatestVersionCache {

  type PartId = String
  type Version = Long

  private object VersionSequence {
    val sequence = new AtomicLong(Random.nextLong())
    val bitsForTs = 41
    val bitsForSeq = 22

    val maskForTs = ~(-1L << bitsForTs)
    val maskForSeq = ~(-1L << bitsForSeq)

    def next: Long = {
      // a timestamp has 41 bits set these days.
      // the version will be 41 bits of this ts + 22 bits of an atomic sequence
      // (first bit is 0 to keep using positives)
      ((System.currentTimeMillis & maskForTs) << (bitsForSeq - 1)) + (sequence.getAndIncrement & maskForSeq)
    }
  }

  /**
   * Generate a shiny new version number, made up of a timestamp and a counter.
   * Over time, version numbers are guaranteed to strictly increase.
   */
  def makeNewVersion: Version = VersionSequence.next
}

/**
 * A cache to hold the latest known versions of parts/parameter values.
 */
trait LatestVersionCache {

  import com.m3.octoparts.cache.versioning.LatestVersionCache._

  /**
   * Get the latest known cache version, if any, of the given part
   */
  def getPartVersion(partId: PartId): Option[Version]

  /**
   * Get the latest known cache version, if any, of the given parameter value
   */
  def getParamVersion(versionedParamKeys: VersionedParamKey): Option[Version]

  /**
   * Update the latest known cache version for the given parameter value
   */
  def updateParamVersion(versionedParamKey: VersionedParamKey, version: Version): Unit

  /**
   * Update the latest known cache version for the given part
   */
  def updatePartVersion(partId: PartId, version: Version): Unit

}
