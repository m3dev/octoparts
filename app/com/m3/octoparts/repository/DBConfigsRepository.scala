package com.m3.octoparts.repository

import com.beachape.logging.LTSVLogger
import com.m3.octoparts.model.config._
import com.m3.octoparts.repository.config._
import play.api.Play
import play.api.libs.concurrent.Akka
import scalikejdbc._
import skinny.orm.SkinnyCRUDMapper
import skinny.orm.feature.associations.Association
import com.m3.octoparts.future.RichFutureWithTiming._

import scala.concurrent.{ Future, blocking }

/**
 * DAO for SkinnyCRUDMapper-based persistence for dependency configs (HttpPartConfig),
 * PartParams, and ThreadPoolConfigs. Imagine this as a wrapper for SkinnyCRUDMapper if that helps.
 *
 * This DAO should be used for modifying data that touch the caching layer instead of the
 * individual DAO companion objects that result from following the pattern imposed by Skinny ORM because
 * it allows us to centralise the cache-decorations in one place. Data that should be retrieved
 * from cache should also be accessed through this DAO
 *
 * The reason why we can't just wrap use generic type-parameterised methods to do our
 * CRUD is because we need to do caching operations (updates/deletes/fetch) of
 * Config objects in specific AND depending on the kind of CRUD, we need to do this differently
 * (deleting or updating ThreadPoolConfigs requires different caching updates
 * compared to doing the same operation on PartParams).
 *
 * Methods come in pair :
 *
 * override def xxx(...)
 * def xxxWithSession(...)(implicit session: DBSession)
 *
 * In the first case, the session is the globally implicit session
 * The second allows to be explicit (for tests)
 */
object DBConfigsRepository extends ImmutableDBRepository with MutableDBRepository with ConfigImporter

object DBContext {

  // A separate ExecutionContext to avoid starving the global one with blocking DB operations
  implicit val dbFetchExecutionContext = Akka.system(Play.current).dispatchers.lookup("contexts.db")

}

trait ImmutableDBRepository extends ConfigsRepository {
  import DBContext._

  // Configs
  def findConfigByPartId(partId: String): Future[Option[HttpPartConfig]] = getWithSession(HttpPartConfigRepository, sqls.eq(HttpPartConfigRepository.defaultAlias.partId, partId), includes = Seq(HttpPartConfigRepository.hystrixConfigRef))

  def findAllConfigs(): Future[Seq[HttpPartConfig]] = getAllWithSession(HttpPartConfigRepository, includes = Seq(HttpPartConfigRepository.hystrixConfigRef))

  def findParamById(id: Long): Future[Option[PartParam]] = getWithSession(PartParamRepository, sqls.eq(PartParamRepository.defaultAlias.id, id), joins = Seq(PartParamRepository.httpPartConfigRef))

  // For ThreadPoolConfigs
  def findThreadPoolConfigById(id: Long): Future[Option[ThreadPoolConfig]] = getWithSession(ThreadPoolConfigRepository, sqls.eq(ThreadPoolConfigRepository.defaultAlias.id, id))

  def findAllThreadPoolConfigs(): Future[Seq[ThreadPoolConfig]] = getAllWithSession(ThreadPoolConfigRepository)

  // For CacheGroups
  def findCacheGroupByName(name: String): Future[Option[CacheGroup]] = getWithSession(CacheGroupRepository, sqls.eq(CacheGroupRepository.defaultAlias.name, name), joins = Seq(CacheGroupRepository.httpPartConfigsRef, CacheGroupRepository.partParamsRef))

  def findAllCacheGroupsByName(names: String*): Future[Seq[CacheGroup]] = if (names.isEmpty) {
    Future.successful(Nil)
  } else {
    getAllByWithSession(CacheGroupRepository, sqls.in(CacheGroupRepository.defaultAlias.name, names), joins = Seq(CacheGroupRepository.httpPartConfigsRef, CacheGroupRepository.partParamsRef))
  }

  def findAllCacheGroups(): Future[Seq[CacheGroup]] = getAllWithSession(CacheGroupRepository, joins = Seq(CacheGroupRepository.httpPartConfigsRef, CacheGroupRepository.partParamsRef))

  /**
   * Gets a single model from a table according to a where clause and logs the where clause used
   */
  private[repository] def getWithSession[A](mapper: SkinnyCRUDMapper[A],
                                            where: SQLSyntax,
                                            joins: Seq[Association[A]] = Nil,
                                            includes: Seq[Association[A]] = Nil)(implicit session: DBSession = ReadOnlyAutoSession): Future[Option[A]] = Future {
    blocking {
      val ret = mapper.joins(joins: _*).includes(includes: _*).findBy(where)
      ret.foreach {
        ci => LTSVLogger.debug("Table" -> mapper.tableName, "where" -> where.toString(), "Data" -> ret.toString)
      }
      ret
    }
  }.measure("DB_GET")

  /**
   * Gets all the records from a table and logs the number of records retrieved
   */
  private[repository] def getAllWithSession[A](mapper: SkinnyCRUDMapper[A],
                                               joins: Seq[Association[A]] = Nil,
                                               includes: Seq[Association[A]] = Nil)(implicit session: DBSession = ReadOnlyAutoSession): Future[Seq[A]] = Future {
    blocking {
      val ret = mapper.joins(joins: _*).includes(includes: _*).findAll()
      LTSVLogger.debug("Table" -> mapper.tableName, "Retrieved records" -> ret.length.toString)
      ret
    }
  }.measure("DB_GET")

  /**
   * Gets all the records from a table according to a where clause and logs the number of records retrieved
   */
  private[repository] def getAllByWithSession[A](mapper: SkinnyCRUDMapper[A],
                                                 where: SQLSyntax,
                                                 joins: Seq[Association[A]] = Nil,
                                                 includes: Seq[Association[A]] = Nil)(implicit session: DBSession = ReadOnlyAutoSession): Future[Seq[A]] = Future {
    blocking {
      val ret = mapper.joins(joins: _*).includes(includes: _*).findAllBy(where)
      LTSVLogger.debug("Table" -> mapper.tableName, "Retrieved records" -> ret.length.toString)
      ret
    }
  }.measure("DB_GET")
}

trait MutableDBRepository extends MutableConfigsRepository {
  import DBContext._

  def save[A <: ConfigModel[A]: ConfigMapper](obj: A): Future[Long] = DB.futureLocalTx { implicit session => saveWithSession(implicitly[ConfigMapper[A]], obj) }

  def deleteAllConfigs(): Future[Int] = deleteAllWithSession(HttpPartConfigRepository)

  def deleteConfigByPartId(partId: String): Future[Int] = deleteWithSession(HttpPartConfigRepository, sqls.eq(HttpPartConfigRepository.defaultAlias.partId, partId))

  def deletePartParamById(id: Long) = deleteWithSession(PartParamRepository, sqls.eq(PartParamRepository.defaultAlias.id, id))

  def deleteThreadPoolConfigById(id: Long) = deleteWithSession(ThreadPoolConfigRepository, sqls.eq(ThreadPoolConfigRepository.defaultAlias.id, id))

  def deleteCacheGroupByName(name: String): Future[Int] = deleteWithSession(CacheGroupRepository, sqls.eq(CacheGroupRepository.defaultAlias.name, name))

  /**
   * Saves a model using the mapper passed in, and logs the data that was saved
   */
  private[repository] def saveWithSession[A <: ConfigModel[A]](mapper: ConfigMapper[A], model: A)(implicit session: DBSession = AutoSession): Future[Long] = Future {
    blocking {
      val id = mapper.save(model)
      LTSVLogger.info("Table" -> mapper.tableName, "Data" -> model.toString, "Action" -> "Saved")
      id
    }
  }.measure("DB_UPDATE")

  /**
   * Deletes using a given Skinny CRUD mapper and an interpolated where clause and logs the where
   * clause used.
   */
  private[repository] def deleteWithSession(mapper: SkinnyCRUDMapper[_], where: SQLSyntax)(implicit session: DBSession = AutoSession): Future[Int] = Future {
    blocking {
      val count = mapper.deleteBy(where)
      LTSVLogger.info("Table" -> mapper.tableName, "where" -> where.toString(), "count" -> count.toString, "Action" -> "Deleted")
      count
    }
  }.measure("DB_UPDATE")

  /**
   * Truncates a table using the passed in Skinny CRUD mapper and logs how many records were deleted
   */
  private[repository] def deleteAllWithSession(mapper: SkinnyCRUDMapper[_])(implicit session: DBSession = AutoSession): Future[Int] = Future {
    blocking {
      val count = mapper.deleteAll
      LTSVLogger.warn("Table" -> mapper.tableName, "count" -> count.toString, "Action" -> "Truncated")
      count
    }
  }.measure("DB_UPDATE")
}

