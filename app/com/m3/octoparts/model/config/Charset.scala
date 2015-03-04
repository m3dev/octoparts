package com.m3.octoparts.model.config

import java.nio.charset.{ Charset => JavaCharset }

object Charset {

  /**
   * Dumb wrapper around [[JavaCharset]].forName
   */
  def forName(name: String): Charset = Charset(JavaCharset.forName(name).name())

}

/**
 * Serialisable version of Java charset
 *
 * The constructor is private so use the companion object's forName method to instantiate one
 */
case class Charset private (name: String) {

  /**
   * The underlying [[JavaCharset]] that actually does real work for you :)
   *
   * This cannot be a val, otherwise it will get serialised.
   */
  def underlying: JavaCharset = JavaCharset.forName(name)

  /**
   * Override for ORM
   */
  override def toString: String = name
}
