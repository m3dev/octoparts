package com.m3.octoparts.repository

import com.m3.octoparts.model.config._
import com.m3.octoparts.repository.config._
import play.api.Play.current
import play.api.libs.concurrent.Akka
import scalikejdbc._
import skinny.logging.Logging
import skinny.orm.SkinnyCRUDMapper
import skinny.orm.feature.associations.Association
import skinny.util.LTSV

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
object DBConfigsRepository extends MutableConfigsRepository with Logging {

  // A separate ExecutionContext to avoid starving the global one with blocking DB operations
  implicit val dbFetchExecutionContext = Akka.system.dispatchers.lookup("contexts.db")

  def save[A <: ConfigModel[A]](obj: A): Future[Long] = {
    DB futureLocalTx { implicit session =>
      saveWithSession(obj.mapper, obj)
    }
  }

  // Configs
  def findConfigByPartId(partId: String): Future[Option[HttpPartConfig]] = {
    getWithSession(
      mapper = HttpPartConfigRepository,
      where = sqls.eq(HttpPartConfigRepository.defaultAlias.partId, partId),
      includes = Seq(HttpPartConfigRepository.hystrixConfigRef))
  }

  def findAllConfigs(): Future[Seq[HttpPartConfig]] = getAllWithSession(
    mapper = HttpPartConfigRepository,
    includes = Seq(HttpPartConfigRepository.hystrixConfigRef))

  def deleteAllConfigs(): Future[Int] = deleteAllWithSession(HttpPartConfigRepository)

  def deleteConfigByPartId(partId: String): Future[Int] = {
    deleteWithSession(HttpPartConfigRepository, sqls.eq(HttpPartConfigRepository.defaultAlias.partId, partId))
  }

  def deletePartParamById(id: Long) = {
    deleteWithSession(PartParamRepository, sqls.eq(PartParamRepository.defaultAlias.id, id))
  }

  def findParamById(id: Long) = {
    getWithSession(
      mapper = PartParamRepository,
      where = sqls.eq(PartParamRepository.defaultAlias.id, id),
      joins = Seq(PartParamRepository.httpPartConfigRef)
    )
  }

  // For ThreadPoolConfigs
  def findThreadPoolConfigById(id: Long): Future[Option[ThreadPoolConfig]] = {
    getWithSession(ThreadPoolConfigRepository, sqls.eq(ThreadPoolConfigRepository.defaultAlias.id, id))
  }

  def findAllThreadPoolConfigs(): Future[Seq[ThreadPoolConfig]] = getAllWithSession(ThreadPoolConfigRepository)

  def deleteThreadPoolConfigById(id: Long) = {
    deleteWithSession(ThreadPoolConfigRepository, sqls.eq(ThreadPoolConfigRepository.defaultAlias.id, id))
  }

  // For CacheGroups
  def findCacheGroupByName(name: String): Future[Option[CacheGroup]] = {
    getWithSession(
      mapper = CacheGroupRepository,
      where = sqls.eq(CacheGroupRepository.defaultAlias.name, name),
      joins = Seq(CacheGroupRepository.httpPartConfigsRef, CacheGroupRepository.partParamsRef)
    )
  }

  def findAllCacheGroupsByName(names: String*): Future[Seq[CacheGroup]] = if (names.isEmpty) {
    Future.successful(Seq.empty)
  } else {
    getAllByWithSession(
      mapper = CacheGroupRepository,
      where = sqls.in(CacheGroupRepository.defaultAlias.name, names),
      joins = Seq(CacheGroupRepository.httpPartConfigsRef, CacheGroupRepository.partParamsRef)
    )
  }

  def findAllCacheGroups(): Future[Seq[CacheGroup]] = {
    getAllWithSession(
      mapper = CacheGroupRepository,
      joins = Seq(CacheGroupRepository.httpPartConfigsRef, CacheGroupRepository.partParamsRef))
  }

  def deleteCacheGroupByName(name: String): Future[Int] = {
    deleteWithSession(CacheGroupRepository, sqls.eq(CacheGroupRepository.defaultAlias.name, name))
  }

  /**
   * Saves a model using the mapper passed in, and logs the data that was saved
   */
  private[repository] def saveWithSession[A <: ConfigModel[A]](mapper: ConfigMapper[A], model: A)(
    implicit session: DBSession = AutoSession): Future[Long] = Future {
    blocking {
      val id = mapper.save(model)
      info(LTSV.dump(
        "Table" -> mapper.tableName,
        "Data" -> model.toString,
        "Action" -> "Saved"
      ))
      id
    }
  }

  /**
   * Deletes using a given Skinny CRUD mapper and an interpolated where clause and logs the where
   * clause used.
   */
  private[repository] def deleteWithSession(mapper: SkinnyCRUDMapper[_], where: SQLSyntax)(
    implicit session: DBSession = AutoSession): Future[Int] = Future {
    blocking {
      val count = mapper.deleteBy(where)
      info(LTSV.dump(
        "Table" -> mapper.tableName,
        "where" -> where.toString(),
        "count" -> count.toString,
        "Action" -> "Deleted"
      ))
      count
    }
  }

  /**
   * Truncates a table using the passed in Skinny CRUD mapper and logs how many records were deleted
   */
  private[repository] def deleteAllWithSession(mapper: SkinnyCRUDMapper[_])(
    implicit session: DBSession = AutoSession): Future[Int] = Future {
    blocking {
      val count = mapper.deleteAll
      warn(LTSV.dump(
        "Table" -> mapper.tableName,
        "count" -> count.toString,
        "Action" -> "Truncated"
      ))
      count
    }
  }

  /**
   * Gets a single model from a table according to a where clause and logs the where clause used
   */
  private[repository] def getWithSession[A](
    mapper: SkinnyCRUDMapper[A], where: SQLSyntax, joins: Seq[Association[A]] = Seq.empty, includes: Seq[Association[A]] = Seq.empty)(
      implicit session: DBSession = ReadOnlyAutoSession): Future[Option[A]] = Future {
    blocking {
      val ret = mapper.joins(joins: _*).includes(includes: _*).findBy(where)
      ret.foreach {
        ci =>
          debug(LTSV.dump(
            "Table" -> mapper.tableName,
            "where" -> where.toString(),
            "Data" -> ret.toString
          ))
      }
      ret
    }
  }

  /**
   * Gets all the records from a table and logs the number of records retreived
   */
  private[repository] def getAllWithSession[A](
    mapper: SkinnyCRUDMapper[A], joins: Seq[Association[A]] = Seq.empty, includes: Seq[Association[A]] = Seq.empty)(
      implicit session: DBSession = ReadOnlyAutoSession): Future[Seq[A]] = Future {
    blocking {
      val ret = mapper.joins(joins: _*).includes(includes: _*).findAll()
      debug(LTSV.dump(
        "Table" -> mapper.tableName,
        "Retrieved records" -> ret.length.toString
      ))
      ret
    }
  }

  /**
   * Gets all the records from a table according to a where clause and logs the number of records retreived
   */
  private[repository] def getAllByWithSession[A](
    mapper: SkinnyCRUDMapper[A], where: SQLSyntax, joins: Seq[Association[A]] = Seq.empty, includes: Seq[Association[A]] = Seq.empty)(
      implicit session: DBSession = ReadOnlyAutoSession): Future[Seq[A]] = Future {
    blocking {
      val ret = mapper.joins(joins: _*).includes(includes: _*).findAllBy(where)
      debug(LTSV.dump(
        "Table" -> mapper.tableName,
        "Retrieved records" -> ret.length.toString
      ))
      ret
    }
  }

}