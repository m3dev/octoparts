package com.m3.octoparts.database

import com.m3.octoparts.support.PlayAppSupport
import org.scalatest.FunSpec

class MigrationsSpec extends FunSpec with PlayAppSupport {

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
