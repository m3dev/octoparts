package com.m3.octoparts.support.db

import com.m3.octoparts.support.PlayAppSupport
import org.scalatest.{ Status, Args, BeforeAndAfterAll, fixture }

/**
 * Trait that supports easy usage of the DB when running tests. Mix into
 * any Spec class extending from FlatSpec for example to get support for
 * implicit session when writing tests
 *
 * Ensures that the test database is torn down and migrated to start with.
 * Each test "fixture" is wrapped in a transaction
 */
trait DBSuite extends AutoRollback2 with BeforeAndAfterAll with PlayAppSupport { this: fixture.Suite =>

  override def beforeAll(): Unit = {
    tearDown()
    migrate()
  }

  /*
   * No-op abstract override to resolve multiple definitions of run in extended traits
   */
  abstract override def run(testName: Option[String], args: Args): Status = super.run(testName, args)
}