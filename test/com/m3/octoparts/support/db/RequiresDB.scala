package com.m3.octoparts.support.db

import com.m3.octoparts.logging.LTSVLogWriter
import com.m3.octoparts.model.config._
import org.flywaydb.core.Flyway
import org.scalatest.{ BeforeAndAfter, Suite }
import org.scalatestplus.play.OneAppPerSuite
import play.api.Logger
import scalikejdbc.ConnectionPool
import skinny.orm.SkinnyMapperBase

import scala.util.control.NonFatal

/**
 * Trait to support tearing down and setting up the test DB.
 *
 * It extends OneAppPerSuite so that we use the Play app's Scalikejdbc connection pool,
 * thereby connecting to the correct DB (as configured in application.test.conf).
 */
trait RequiresDB extends Suite with OneAppPerSuite {

  lazy val flyway = {
    val poolName = ConnectionPool.DEFAULT_NAME.name
    val pool = ConnectionPool.get(Symbol(poolName))
    val flyway = new Flyway
    flyway.setDataSource(pool.dataSource)
    flyway.setPlaceholderPrefix("$flyway{")
    flyway
  }

  // Start the Scalikejdbc connection pool
  //DBs.setupAll()

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
      case NonFatal(e) => LTSVLogWriter.error(e)
    }
  }

  /**
   * Method to migrate the database from the migration files in src/main/resources/db/migration
   */
  def migrate(): Int = {
    flyway.migrate()
  }
}
