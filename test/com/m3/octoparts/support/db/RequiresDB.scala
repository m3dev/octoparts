package com.m3.octoparts.support.db

import java.sql.Connection

import com.beachape.logging.LTSVLogger
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationInfo
import org.flywaydb.core.api.callback.FlywayCallback
import org.scalatest.Suite
import org.scalatestplus.play.OneAppPerSuite
import scalikejdbc.ConnectionPool

import scala.util.control.NonFatal

/**
 * Trait to support tearing down and setting up the test DB.
 *
 * It extends OneAppPerSuite so that we use the Play app's Scalikejdbc connection pool,
 * thereby connecting to the correct DB (as configured in application.test.conf).
 */
trait RequiresDB extends Suite with OneAppPerSuite {

  lazy val flyway = {
    val flyway = new Flyway
    flyway.setCallbacks(new FlywayCallback {
      def beforeInit(conn: Connection) = conn.setReadOnly(false)

      def beforeRepair(conn: Connection) = conn.setReadOnly(false)

      def beforeValidate(conn: Connection) = conn.setReadOnly(false)

      def beforeInfo(conn: Connection) = conn.setReadOnly(false)

      def beforeClean(conn: Connection) = conn.setReadOnly(false)

      def beforeMigrate(conn: Connection) = conn.setReadOnly(false)

      def beforeBaseline(conn: Connection) = conn.setReadOnly(false)

      def beforeEachMigrate(conn: Connection, p2: MigrationInfo) = conn.setReadOnly(false)

      def afterInfo(conn: Connection) = {}

      def afterInit(conn: Connection) = {}

      def afterRepair(conn: Connection) = {}

      def afterValidate(conn: Connection) = {}

      def afterEachMigrate(conn: Connection, p2: MigrationInfo) = {}

      def afterMigrate(conn: Connection) = {}

      def afterClean(conn: Connection) = {}

      def afterBaseline(conn: Connection) = {}
    })
    flyway.setDataSource(ConnectionPool().dataSource)
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
