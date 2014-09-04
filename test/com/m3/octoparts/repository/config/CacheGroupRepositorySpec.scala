package com.m3.octoparts.repository.config

import com.m3.octoparts.model.config.CacheGroup
import com.m3.octoparts.support.db.DBSuite
import com.m3.octoparts.support.mocks.ConfigDataMocks
import org.joda.time.DateTime
import org.scalatest.{ Matchers, fixture }

class CacheGroupRepositorySpec extends fixture.FunSpec with DBSuite with Matchers with ConfigDataMocks {

  describe(".save") {

    val exampleCacheGroup = CacheGroup(name = "helloCache", owner = "me", createdAt = DateTime.now, updatedAt = DateTime.now)

    describe("to insert a CacheGroup") {

      it("should return an id that can be used for retrieving the cache group") { implicit s =>
        val id = CacheGroupRepository.save(exampleCacheGroup)
        val retrieved = CacheGroupRepository.findById(id).head
        retrieved.name should be(exampleCacheGroup.name)
      }

    }

    describe("to save an already-persisted CacheGroup") {

      it("should return an id that can be used for retrieving the cache group") { implicit s =>
        val id = CacheGroupRepository.save(exampleCacheGroup)
        val retrieved = CacheGroupRepository.findById(id).head
        val updatedCG = retrieved.copy(name = "helloCacheUpdated")
        val updateId = CacheGroupRepository.save(updatedCG)
        val retrieved2 = CacheGroupRepository.findById(updateId).head
        retrieved2.name should be(updatedCG.name)
      }

    }

  }

}
