package com.m3.octoparts.model.config

import com.m3.octoparts.repository.config.ConfigMapper

/**
 * The base trait for our models.
 *
 * For simplicity, we say that every model needs to have an Option[Long] id
 * field so that we can tell if it is new or old, thereby making it obvious
 * what we should do when trying to "save" a model
 */
trait ConfigModel[A <: ConfigModel[A]] {

  /**
   * Identifier.
   */
  def id: Option[Long]

  /**
   * Used for easy access to a mapper of an object.
   *
   * For our purposes, it's much easier and faster to implement this in code within
   * implementing classes than to use tricks like implicits (requires more boiler
   * plate) and reflection (runtime cost)
   */
  def mapper: ConfigMapper[A]

}
