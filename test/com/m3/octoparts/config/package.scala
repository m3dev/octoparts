package com.m3.octoparts

import java.util.UUID
import org.joda.time.DateTime

package object config {
  def newId: String = UUID.randomUUID.toString
  def now: DateTime = DateTime.now
}
