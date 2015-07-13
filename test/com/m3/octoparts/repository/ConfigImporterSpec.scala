package com.m3.octoparts.repository

import com.beachape.zipkin.services.NoopZipkinService
import com.m3.octoparts.model.config.HttpPartConfig
import com.m3.octoparts.repository.config.{ ThreadPoolConfigRepository, HystrixConfigRepository }
import com.m3.octoparts.support.db.DBSuite
import com.m3.octoparts.support.mocks.ConfigDataMocks
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatest.{ Matchers, fixture }

class ConfigImporterSpec extends fixture.FlatSpec with DBSuite with Matchers with ConfigDataMocks with ScalaFutures with IntegrationPatience {

  def completeCfg(partId: String) = mockHttpPartConfig.copy(partId = partId, hystrixConfig = Some(mockHystrixConfig.copy(commandKey = partId)))
  lazy val dbConfigRepo = new DBConfigsRepository(NoopZipkinService, scala.concurrent.ExecutionContext.global)

  it should "not import more than 1 config per partId" in {
    implicit session =>
      val configs = Seq(completeCfg("partId"), completeCfg("partId"), completeCfg("partId"))
      whenReady(dbConfigRepo.importConfigsWithSession(configs.map(HttpPartConfig.toJsonModel))) {
        partIds =>
          partIds should have size 1
          partIds.head should be("partId")
      }
  }

  it should "fail to import, because the hystrix config is already registered" in {
    implicit session =>
      whenReady(dbConfigRepo.importConfigsWithSession(Seq(completeCfg("partId")).map(HttpPartConfig.toJsonModel))) {
        partIds =>
          // provoke a name collision with command keys
          session.executeUpdate(s"UPDATE ${HystrixConfigRepository.tableName} SET command_key = ? WHERE command_key = ?", "bleargh", "partId")
          val configs = Seq(completeCfg("bleargh"))
          whenReady(dbConfigRepo.importConfigsWithSession(configs.map(HttpPartConfig.toJsonModel)).failed) { e =>
            e shouldBe an[IllegalArgumentException]
          }
      }
  }

  it should "import all dependencies correctly" in {
    implicit session =>
      val configs = Seq(
        completeCfg("0"),
        completeCfg("1"),
        completeCfg("2")
      )
      whenReady(dbConfigRepo.importConfigsWithSession(configs.map(HttpPartConfig.toJsonModel))) {
        partIds =>
          // order may get jumbled
          partIds.toSet should equal(configs.map(_.partId).toSet)

          whenReady(dbConfigRepo.getAllWithSession(ThreadPoolConfigRepository)) { tpcs =>
            tpcs.filter(_.threadPoolKey == "testThreadPool") should have size 1
          }
      }
  }
}
