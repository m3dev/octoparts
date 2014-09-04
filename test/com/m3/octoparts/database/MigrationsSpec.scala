package com.m3.octoparts.database

import org.scalatest.FunSpec
import com.m3.octoparts.support.db.RequiresDB

class MigrationsSpec extends FunSpec with RequiresDB {

  describe("Running migrations") {
    describe("from an empty database") {
      it("should not fail") {
        tearDown()
        migrate()
      }
    }

    describe("from a fully migrated database") {
      it("should not fail") {
        migrate()
        migrate()
      }
    }

  }

}
