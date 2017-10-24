package com.m3.octoparts.support.db

import com.beachape.logging.LTSVLogger
import org.flywaydb.core.Flyway
import org.scalatest.Suite
import scalikejdbc.ConnectionPool

import scala.util.control.NonFatal

/**
 * Trait to support tearing down and setting up the test DB.
 *
 * Currently private to the [support] package because assumes that it is mixed into a test suite
 * that ensures an app will be lazily set up so the stateful ConnectionPool singleton is initalised.
 *
 *
 */
private[support] trait RequiresDB extends Suite {

  def flyway = {
    val flyway = new Flyway
    flyway.setDataSource(ConnectionPool().dataSource)
    flyway.setPlaceholderPrefix("$flyway{")
    flyway
  }

  /**
   * Method to tear down the entire database corresponding to this environment
   *
   * Wrapped entirely in a try-catch because tearDown is only done for just-in-case
   * resetting purposes
   */
  def tearDown(): Unit = {
    try {
      /*
            Tries to drop all objects in the database.

            Needs to be in this try because it throws the database doesn't have the schema_version
            table, which may happen if in an arbitrary test, someone decides to drop everything in the
            database directly without initiating it again.
           */
      flyway.clean()
    } catch {
      case NonFatal(e) => LTSVLogger.error(e)
    }
  }

  /**
   * Method to migrate the database from the migration files in src/main/resources/db/migration
   */
  def migrate(): Int = {
    flyway.migrate()
  }

}
