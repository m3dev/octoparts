package com.m3.octoparts.repository

import com.beachape.logging.LTSVLogger
import com.beachape.zipkin.FutureEnrichment._
import com.beachape.zipkin.services.ZipkinServiceLike
import com.kenshoo.play.metrics.Metrics
import com.m3.octoparts.future.RichFutureWithTiming._
import com.m3.octoparts.model.config._
import com.m3.octoparts.repository.config._
import com.twitter.zipkin.gen.Span
import scalikejdbc._
import skinny.orm.SkinnyCRUDMapper
import skinny.orm.feature.CRUDFeatureWithId
import skinny.orm.feature.associations.Association

import scala.collection.SortedSet
import scala.concurrent.{ ExecutionContext, Future, blocking }

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
class DBConfigsRepository(zipkinServiceFactory: => ZipkinServiceLike,
                          protected val executionContext: ExecutionContext)(implicit val metrics: Metrics)
  extends ImmutableDBRepository
  with MutableDBRepository
  with ConfigImporter {

  implicit lazy val zipkinService: ZipkinServiceLike = zipkinServiceFactory
}

trait ImmutableDBRepository extends ConfigsRepository {

  protected implicit def executionContext: ExecutionContext

  implicit def zipkinService: ZipkinServiceLike

  implicit def metrics: Metrics

  private val zipkinSpanNameBase = "db-repo-read"

  // Configs
  def findConfigByPartId(partId: String)(implicit parentSpan: Span): Future[Option[HttpPartConfig]] = {
    // a hack to set the part parameter cache groups
    // Since there is no reference in the PartParam object, the simplest way is to simply fetch all cache groups.
    // They should be cached anyways.
    val fMbPartConfig = getWithSession(HttpPartConfigRepository, sqls.eq(HttpPartConfigRepository.defaultAlias.partId, partId), includes = Seq(HttpPartConfigRepository.hystrixConfigRef))
      .trace(s"$zipkinSpanNameBase-findConfigByPartId", "partId" -> partId)
    val fCacheGroups = findAllCacheGroups()
    for {
      cacheGroups <- fCacheGroups
      mbPartConfig <- fMbPartConfig
    } yield {
      mbPartConfig.map {
        config =>
          config.copy(parameters = config.parameters.map {
            param => param.copy(cacheGroups = cacheGroups.filter(_.partParams.exists(_.id == param.id)))
          })
      }
    }
  }

  def findAllConfigs()(implicit parentSpan: Span): Future[SortedSet[HttpPartConfig]] = {
    // a hack to set the part parameter cache groups. see explanation in #findConfigByPartId
    val fCacheGroups = findAllCacheGroups()
    val fConfigs = getAllWithSession(HttpPartConfigRepository, includes = Seq(HttpPartConfigRepository.hystrixConfigRef))
      .trace(s"$zipkinSpanNameBase-findAllConfigs")
    for {
      configs <- fConfigs
      allCacheGroups <- fCacheGroups
    } yield {
      configs.map {
        config =>
          config.copy(parameters = config.parameters.map {
            param => param.copy(cacheGroups = allCacheGroups.filter(_.partParams.exists(_.id == param.id)))
          })
      }
    }
  }

  def findParamById(id: Long)(implicit parentSpan: Span): Future[Option[PartParam]] = {
    getWithSession(PartParamRepository, sqls.eq(PartParamRepository.defaultAlias.id, id), joins = Seq(PartParamRepository.httpPartConfigRef, PartParamRepository.cacheGroupsRef))
      .trace(s"$zipkinSpanNameBase-findParamById", "id" -> id.toString)
  }

  // For ThreadPoolConfigs
  def findThreadPoolConfigById(id: Long)(implicit parentSpan: Span): Future[Option[ThreadPoolConfig]] = {
    getWithSession(ThreadPoolConfigRepository, sqls.eq(ThreadPoolConfigRepository.defaultAlias.id, id), includes = Seq(ThreadPoolConfigRepository.hystrixConfigRef))
      .trace(s"$zipkinSpanNameBase-findThreadPoolConfigById", "id" -> id.toString)
  }

  def findAllThreadPoolConfigs()(implicit parentSpan: Span): Future[SortedSet[ThreadPoolConfig]] = {
    getAllWithSession(ThreadPoolConfigRepository, includes = Seq(ThreadPoolConfigRepository.hystrixConfigRef))
      .trace(s"$zipkinSpanNameBase-findAllThreadPoolConfigs")
  }

  // For CacheGroups
  def findCacheGroupByName(name: String)(implicit parentSpan: Span): Future[Option[CacheGroup]] = {
    for {
      mbCacheGroup <- getWithSession(CacheGroupRepository, sqls.eq(CacheGroupRepository.defaultAlias.name, name), joins = Seq(CacheGroupRepository.httpPartConfigsRef, CacheGroupRepository.partParamsRef))
        .trace(s"$zipkinSpanNameBase-findCacheGroupByName", "name" -> name)
      fPopulateCacheGroupPartParams: Seq[Future[CacheGroup]] = mbCacheGroup.toSeq.map(populateCacheGroupPartParams)
      populatedCacheGroups <- Future.sequence(fPopulateCacheGroupPartParams)
    } yield {
      populatedCacheGroups.headOption
    }
  }

  def findAllCacheGroupsByName(names: String*)(implicit parentSpan: Span): Future[SortedSet[CacheGroup]] = if (names.isEmpty) {
    Future.successful(SortedSet.empty)
  } else {
    for {
      cacheGroups <- getAllByWithSession(CacheGroupRepository, sqls.in(CacheGroupRepository.defaultAlias.name, names), joins = Seq(CacheGroupRepository.httpPartConfigsRef, CacheGroupRepository.partParamsRef))
        .trace(s"$zipkinSpanNameBase-findAllCacheGroupsByName", "names" -> names.toString)
      fPopulateCacheGroupPartParams: Seq[Future[CacheGroup]] = cacheGroups.toSeq.map(populateCacheGroupPartParams)
      populatedCacheGroups <- Future.sequence(fPopulateCacheGroupPartParams)
    } yield {
      populatedCacheGroups.to[SortedSet]
    }
  }

  def findAllCacheGroups()(implicit parentSpan: Span): Future[SortedSet[CacheGroup]] = {
    for {
      cacheGroups <- getAllWithSession(CacheGroupRepository, joins = Seq(CacheGroupRepository.httpPartConfigsRef, CacheGroupRepository.partParamsRef))
        .trace(s"$zipkinSpanNameBase-findAllCacheGroups")
      fPopulateCacheGroupPartParams: Seq[Future[CacheGroup]] = cacheGroups.toSeq.map(populateCacheGroupPartParams)
      populatedCacheGroups <- Future.sequence(fPopulateCacheGroupPartParams)
    } yield {
      populatedCacheGroups.to[SortedSet]
    }
  }

  /**
   * AFAIK the ORM could not be twisted to fetch cacheGroup.partParam.httpPartConfig.
   * This might make 1 request per member, but [[findParamById]] is cached.
   */
  private def populateCacheGroupPartParams(cacheGroup: CacheGroup)(implicit parentSpan: Span): Future[CacheGroup] = {
    val fOptParams: Seq[Future[Option[PartParam]]] = for {
      partParam <- cacheGroup.partParams.toSeq
      paramId <- partParam.id.toSeq
    } yield {
      findParamById(paramId)
    }

    for {
      optParams <- Future.sequence(fOptParams)
    } yield {
      cacheGroup.copy(partParams = optParams.flatten.to[SortedSet])
    }
  }

  /**
   * Gets a single model from a table according to a where clause and logs the where clause used
   */
  private[repository] def getWithSession[A](mapper: CRUDFeatureWithId[Long, A],
                                            where: SQLSyntax,
                                            joins: Seq[Association[_]] = Nil,
                                            includes: Seq[Association[_]] = Nil)(implicit session: DBSession = ReadOnlyAutoSession): Future[Option[A]] = Future {
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
  private[repository] def getAllWithSession[A: Ordering](mapper: CRUDFeatureWithId[Long, A],
                                                         joins: Seq[Association[_]] = Nil,
                                                         includes: Seq[Association[_]] = Nil)(implicit session: DBSession = ReadOnlyAutoSession): Future[SortedSet[A]] =
    Future {
      blocking {
        val ret = mapper.joins(joins: _*).includes(includes: _*).findAll().to[SortedSet]
        LTSVLogger.debug("Table" -> mapper.tableName, "Retrieved records" -> ret.size.toString)
        ret
      }
    }.measure("DB_GET")

  /**
   * Gets all the records from a table according to a where clause and logs the number of records retrieved
   */
  private[repository] def getAllByWithSession[A: Ordering](mapper: CRUDFeatureWithId[Long, A],
                                                           where: SQLSyntax,
                                                           joins: Seq[Association[_]] = Nil,
                                                           includes: Seq[Association[_]] = Nil)(implicit session: DBSession = ReadOnlyAutoSession): Future[SortedSet[A]] = Future {
    blocking {
      val ret = mapper.joins(joins: _*).includes(includes: _*).findAllBy(where).to[SortedSet]
      LTSVLogger.debug("Table" -> mapper.tableName, "Retrieved records" -> ret.size.toString)
      ret
    }
  }.measure("DB_GET")
}

trait MutableDBRepository extends MutableConfigsRepository {

  protected implicit def executionContext: ExecutionContext

  implicit def zipkinService: ZipkinServiceLike
  implicit def metrics: Metrics
  private val zipkinSpanNameBase = "db-repo-mutation"

  def save[A <: ConfigModel[A]: ConfigMapper](obj: A)(implicit parentSpan: Span): Future[Long] = {
    DB.futureLocalTx { implicit session => saveWithSession(implicitly[ConfigMapper[A]], obj) }.trace(s"$zipkinSpanNameBase-save", "object" -> obj.toString)
  }

  def deleteAllConfigs()(implicit parentSpan: Span): Future[Int] = {
    deleteAllWithSession(HttpPartConfigRepository).trace(s"$zipkinSpanNameBase-deleteAllConfigs")
  }

  def deleteConfigByPartId(partId: String)(implicit parentSpan: Span): Future[Int] = {
    deleteWithSession(HttpPartConfigRepository, sqls.eq(HttpPartConfigRepository.defaultAlias.partId, partId))
      .trace(s"$zipkinSpanNameBase-deleteConfigByPartId", "partId" -> partId)
  }

  def deletePartParamById(id: Long)(implicit parentSpan: Span) = {
    deleteWithSession(PartParamRepository, sqls.eq(PartParamRepository.defaultAlias.id, id))
      .trace(s"$zipkinSpanNameBase-deletePartParamById", "id" -> id.toString)
  }

  def deleteThreadPoolConfigById(id: Long)(implicit parentSpan: Span) = {
    deleteWithSession(ThreadPoolConfigRepository, sqls.eq(ThreadPoolConfigRepository.defaultAlias.id, id))
      .trace(s"$zipkinSpanNameBase-deleteThreadPoolConfigById", "id" -> id.toString)
  }

  def deleteCacheGroupByName(name: String)(implicit parentSpan: Span): Future[Int] = {
    deleteWithSession(CacheGroupRepository, sqls.eq(CacheGroupRepository.defaultAlias.name, name))
      .trace(s"$zipkinSpanNameBase-deleteCacheGroupByName", "name" -> name)
  }

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

