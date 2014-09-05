package com.m3.octoparts.model.config

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

}
