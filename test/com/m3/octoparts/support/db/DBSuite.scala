package com.m3.octoparts.support.db

import org.scalatest.BeforeAndAfterAll
import org.scalatest.fixture.Suite

/**
 * Trait that supports easy usage of the DB when running tests. Mix into
 * any Spec class extending from FlatSpec for example to get support for
 * implicit session when writing tests
 *
 * Ensures that the test database is torn down and migrated to start with.
 * Each test "fixture" is wrapped in a transaction
 */
trait DBSuite extends AutoRollback2 with BeforeAndAfterAll with RequiresDB { this: Suite =>

  override def beforeAll(): Unit = {
    tearDown()
    migrate()
  }
}