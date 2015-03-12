package com.m3.octoparts.repository

import com.beachape.zipkin.services.NoopZipkinService
import com.m3.octoparts.repository.config._
import com.twitter.zipkin.gen.Span
import org.scalatest._
import com.m3.octoparts.support.db._
import com.m3.octoparts.support.mocks.ConfigDataMocks
import scalikejdbc._
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.duration._
import scala.language.postfixOps

class DBConfigsRepositorySpec extends fixture.FlatSpec with DBSuite with Matchers with ConfigDataMocks with ScalaFutures {

  implicit val p = PatienceConfig(timeout = 5 seconds)
  implicit val zipkinService = NoopZipkinService
  implicit val emptySpan = new Span()
  lazy val dbConfigRepo = new DBConfigsRepository()

  behavior of "DBConfigsRepository"

  it should "not contain the test ConfigItem at first" in {
    implicit session =>
      whenReady(dbConfigRepo.getAllWithSession(HttpPartConfigRepository)) {
        _.find(_.partId == mockHttpPartConfig.partId) should not be 'defined
      }
  }

  it should "contain the test ConfigItem after insertion" in {
    implicit session =>
      whenReady(dbConfigRepo.saveWithSession(HttpPartConfigRepository, mockHttpPartConfig)) {
        saved =>
          // Get ALL should work
          whenReady(dbConfigRepo.getAllWithSession(HttpPartConfigRepository)) {
            _.find(_.partId == mockHttpPartConfig.partId) should be('defined)
          }
          // Get to find 1 item should work
          whenReady(dbConfigRepo.getWithSession(HttpPartConfigRepository, sqls.eq(HttpPartConfigRepository.defaultAlias.partId, mockHttpPartConfig.partId))) {
            _ should not be 'empty
          }
      }
  }

  it should "not contain the test ConfigItem after insertion followed by deletion by its partId" in {
    implicit session =>
      whenReady(dbConfigRepo.saveWithSession(HttpPartConfigRepository, mockHttpPartConfig)) {
        saved =>
          whenReady(dbConfigRepo.deleteWithSession(HttpPartConfigRepository, sqls.eq(HttpPartConfigRepository.defaultAlias.partId, mockHttpPartConfig.partId))) {
            deleted =>
              whenReady(dbConfigRepo.getAllWithSession(HttpPartConfigRepository)) {
                _.find(_.partId == mockHttpPartConfig.partId) should not be 'defined
              }
          }
      }
  }

  it should "not contain the test ConfigItem after insertion followed by deletion of all items" in {
    implicit session =>
      whenReady(dbConfigRepo.saveWithSession(HttpPartConfigRepository, mockHttpPartConfig)) {
        saved =>
          whenReady(dbConfigRepo.deleteAllWithSession(HttpPartConfigRepository)) {
            deleted =>
              whenReady(dbConfigRepo.getAllWithSession(HttpPartConfigRepository)) {
                _ should be('empty)
              }
          }
      }
  }

  it should "have a working findAllCacheGroupsByName" in {
    implicit session =>
      whenReady(dbConfigRepo.save(mockCacheGroup)) {
        savedId =>
          whenReady(dbConfigRepo.findAllCacheGroupsByName(mockCacheGroup.name)) {
            seqCacheGroups =>
              val group = seqCacheGroups.head
              group.name should be(mockCacheGroup.name)
          }
          whenReady(dbConfigRepo.findAllCacheGroupsByName()) {
            _ should be('empty)
          }
      }
  }

}

