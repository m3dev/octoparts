package com.m3.octoparts.repository

import com.beachape.logging.LTSVLogger
import com.m3.octoparts.model.config.json
import com.m3.octoparts.model.config._
import com.m3.octoparts.repository.config._
import scalikejdbc._

import scala.concurrent.Future

trait ConfigImporter {
  self: MutableDBRepository with ImmutableDBRepository =>

  import com.m3.octoparts.repository.DBContext._

  def importConfigs(configs: Seq[json.HttpPartConfig]) = DB.futureLocalTx { implicit session => importConfigsWithSession(configs) }

  private[repository] object ImportAction {

    def distinctBy[A, B](items: Seq[A])(by: A => B): Seq[A] = items.groupBy(by).mapValues(_.head).values.toSeq
  }

  private[repository] class ImportAction(configs: Seq[json.HttpPartConfig])(implicit session: DBSession) {

    import ImportAction._

    val uniqueConfigs = distinctBy(configs)(_.partId)

    val fThreadPoolConfigs: Future[Map[String, Long]] = {
      /**
       * If a [[ThreadPoolConfig]] with the same [[ThreadPoolConfig.threadPoolKey]] exists in DB, returns its ID.
       * Else, inserts a new [[ThreadPoolConfig]] and returns its ID
       */
      def insertThreadPoolConfigIfMissing(threadPoolConfig: json.ThreadPoolConfig)(implicit session: DBSession): Future[(String, Long)] = {
        val oldThreadPoolConfig = getWithSession(ThreadPoolConfigRepository, sqls.eq(ThreadPoolConfigRepository.defaultAlias.threadPoolKey, threadPoolConfig.threadPoolKey))
        oldThreadPoolConfig.flatMap { mbTpcWasThere =>
          mbTpcWasThere.fold {
            saveWithSession(ThreadPoolConfigRepository, ThreadPoolConfig.fromJsonModel(threadPoolConfig)).map(threadPoolConfig.threadPoolKey -> _)
          } {
            tpcWasThere =>
              LTSVLogger.debug("Thread pool" -> tpcWasThere.threadPoolKey, "Action" -> "Skipping import")
              Future.successful(threadPoolConfig.threadPoolKey -> tpcWasThere.id.get)
          }
        }
      }

      val threadPoolConfigs = distinctBy(uniqueConfigs.map(_.hystrixConfig.threadPoolConfig))(_.threadPoolKey)
      Future.sequence(threadPoolConfigs.map(insertThreadPoolConfigIfMissing)).map(_.toMap)
    }

    val fCacheGroups: Future[Map[String, CacheGroup]] = {

      /**
       * If a [[CacheGroup]] with the same name exists in DB, returns it.
       * Else, inserts a new [[CacheGroup]] and returns it
       */
      def insertCacheGroupIfMissing(jCacheGroup: json.CacheGroup)(implicit session: DBSession): Future[(String, CacheGroup)] = {
        val oldCacheGroupConfig = getWithSession(CacheGroupRepository, sqls.eq(CacheGroupRepository.defaultAlias.name, jCacheGroup.name))
        oldCacheGroupConfig.flatMap { mbCgWasThere =>
          mbCgWasThere.fold {
            val fCacheGroupId = saveWithSession(CacheGroupRepository, CacheGroup.fromJsonModel(jCacheGroup))
            fCacheGroupId.flatMap { cacheGroupId =>
              getWithSession(CacheGroupRepository, sqls.eq(CacheGroupRepository.defaultAlias.id, cacheGroupId)).map(jCacheGroup.name -> _.get)
            }
          } { cgWasThere =>
            LTSVLogger.debug("Cache group" -> cgWasThere.name, "Action" -> "Skipping import")
            Future.successful(cgWasThere.name -> cgWasThere)
          }
        }
      }

      val cacheGroups = distinctBy(uniqueConfigs.flatMap(_.cacheGroups.toSeq) ++ uniqueConfigs.flatMap(_.parameters).flatMap(_.cacheGroups))(_.name)
      Future.sequence(cacheGroups.map(insertCacheGroupIfMissing)).map(_.toMap)
    }

    def doImport() = {
      Future.sequence(uniqueConfigs.map(insertConfigIfMissing)).map(_.flatten)
    }

    /**
     * Unconditionnally inserts a [[HystrixConfig]], also inserting a [[ThreadPoolConfig]] if necessary
     * @return inserted [[HystrixConfig.id]], or Future.failed with a IllegalArgumentException if a [[HystrixConfig]] with the same [[HystrixConfig.commandKey]] already exists
     */
    private[repository] def insertHystrixConfig(configId: Long, jHystrixConfig: json.HystrixConfig)(implicit session: DBSession): Future[Long] = {
      fThreadPoolConfigs.flatMap { threadPoolConfigsWithIds =>
        val nakedHystrixConfig = HystrixConfig.fromJsonModel(jHystrixConfig).copy(
          httpPartConfigId = Some(configId),
          httpPartConfig = None,
          threadPoolConfigId = threadPoolConfigsWithIds.get(jHystrixConfig.threadPoolConfig.threadPoolKey),
          threadPoolConfig = None)
        getWithSession(HystrixConfigRepository, sqls.eq(HystrixConfigRepository.defaultAlias.commandKey, nakedHystrixConfig.commandKey)).flatMap {
          _.fold(saveWithSession(HystrixConfigRepository, nakedHystrixConfig)) {
            existingHystrixConfig => throw new IllegalArgumentException(s"There is already a HystrixConfig with key=${existingHystrixConfig.commandKey}")
          }
        }
      }
    }

    /**
     * Inserts a [[PartParam]] and, if necessary, its related [[CacheGroup]]s.
     * @return inserted partParam ID
     */
    private[repository] def insertPartParam(configId: Long, partParam: json.PartParam)(implicit session: DBSession): Future[Long] = {
      fCacheGroups.flatMap { cacheGroups =>
        // keep cache groups here
        val nakedPartParam = PartParam.fromJsonModel(partParam).copy(
          httpPartConfigId = Some(configId),
          httpPartConfig = None,
          cacheGroups = partParam.cacheGroups.map(_.name).flatMap(cacheGroups.get).toSet)
        saveWithSession(PartParamRepository, nakedPartParam)
      }
    }

    /**
     * Inserts a [[HttpPartConfig]]
     * @return the [[HttpPartConfig.partId]] if inserted, else [[None]]
     */
    private[repository] def insertConfigIfMissing(jpart: json.HttpPartConfig)(implicit session: DBSession): Future[Option[String]] = {
      LTSVLogger.info("part" -> jpart.toString, "action" -> "insert if missing")

      val oldConfig = getWithSession(HttpPartConfigRepository, sqls.eq(HttpPartConfigRepository.defaultAlias.partId, jpart.partId))
      oldConfig.flatMap { mbConfigWasThere =>
        mbConfigWasThere.fold {
          fCacheGroups.flatMap { cacheGroups =>
            val nakedConfig = HttpPartConfig.fromJsonModel(jpart).copy(
              parameters = Set.empty,
              hystrixConfig = None,
              cacheGroups = jpart.cacheGroups.map(_.name).flatMap(cacheGroups.get).toSet)
            saveAndReturnId(jpart, nakedConfig)
          }
        } {
          configWasThere => Future.successful(None)
        }
      }
    }

    /**
     * save and immediately retrieve the new config to get its ID
     */
    private[repository] def saveAndReturnId(jpart: json.HttpPartConfig, nakedConfig: HttpPartConfig)(implicit session: DBSession) = {
      saveWithSession(HttpPartConfigRepository, nakedConfig).flatMap {
        id => getWithSession(HttpPartConfigRepository, sqls.eq(HttpPartConfigRepository.defaultAlias.id, id))
      }.flatMap { mbInserted =>
        mbInserted.map {
          insertedConfig =>
            // insert dependencies
            val configId = insertedConfig.id.get
            val fInsertParameters = jpart.parameters.map { partParam => insertPartParam(configId, partParam) }
            // must have a hystrix config
            val fInsertHystrixConfig = insertHystrixConfig(configId, jpart.hystrixConfig)
            Future.sequence(fInsertParameters + fInsertHystrixConfig).map { _ => Option(insertedConfig.partId) }
        }.getOrElse {
          Future.failed(new IllegalStateException(s"Just inserted a new config ${jpart.partId} but it was not saved!"))
        }
      }
    }
  }

  private[repository] def importConfigsWithSession(configs: Seq[json.HttpPartConfig])(implicit session: DBSession = AutoSession) = {
    // this must be done beforehand because imports will be run in parallel
    // insert thread pools first
    new ImportAction(configs).doImport()
  }
}
