package com.m3.octoparts.support.db

import org.scalatest.Outcome
import org.scalatest.fixture.Suite

import scalikejdbc._

/**
 * Adapted from scalikejdbc's AutoRollback for scalatest 2
 */
trait AutoRollback2 { self: Suite =>

  type FixtureParam = DBSession

  /**
   * Creates a [[scalikejdbc.DB]] instance.
   * @return DB instance
   */
  def db(): DB = {
    DB(ConnectionPool.borrow())
  }

  /**
   * Prepares database for the test.
   * @param session db session implicitly
   */
  def fixture(implicit session: DBSession): Unit = {}

  /**
   * Provides transactional block
   * @param test one arg test
   */
  override def withFixture(test: OneArgTest): Outcome = {
    using(db()) { db =>
      try {
        db.begin()
        db.withinTx { implicit session =>
          fixture(session)
        }
        test(db.withinTxSession())
      } finally {
        db.rollbackIfActive()
      }
    }
  }

}
