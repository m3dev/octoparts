package com.m3.octoparts.repository

import com.m3.octoparts.cache.Cache
import com.m3.octoparts.cache.key.{ CacheGroupCacheKey, CacheKey, HttpPartConfigCacheKey }
import com.m3.octoparts.http.HttpClientPool
import com.beachape.logging.LTSVLogger
import shade.memcached.MemcachedCodecs._

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps
import scala.util.control.NonFatal

trait CachingRepository extends ConfigsRepository {
  implicit def executionContext: ExecutionContext

  def delegate: ConfigsRepository

  def cache: Cache

  def httpClientPool: HttpClientPool

  // Warm up the cache on initialisation of this wrapper
  reloadCache()

  /**
   * Generic method for putting something into cache using any kind of identifier
   */
  def put[A](cacheKey: CacheKey, maybeCacheable: Option[A]): Future[Unit] = {
    cache.put(cacheKey, maybeCacheable, Some(Duration.Inf)).recover {
      // no need to propagate cache put errors
      case NonFatal(cacheFailure) =>
        LTSVLogger.error(cacheFailure, "Cache put" -> cacheKey.toString)
    }
  }

  /**
   * Reloads the cache using objects fetched from the delegate
   *
   * If we eventually add more objects that need to be cached, simply follow the pattern
   */
  protected def reloadCache(): Future[Seq[Unit]] = {
    LTSVLogger.info("Reloading configs cache")
    val reloadFSeqs = Seq(
      findAllAndCache(findAllConfigs())(c => HttpPartConfigCacheKey(c.partId)),
      findAllAndCache(findAllCacheGroups())(cG => CacheGroupCacheKey(cG.name))
    )
    Future.sequence(reloadFSeqs).map(_.flatten)
  }

  /**
   * Given a function that returns a Future Sequence of A and a function that turns
   * each A into a CacheKey, performs a find-and-cache
   */
  private def findAllAndCache[A](findAll: => Future[Seq[A]])(cacheKey: A => CacheKey): Future[Seq[Unit]] = {
    for {
      seq <- findAll
      seqFPut <- Future.sequence(seq.map(obj => put(cacheKey(obj), Some(obj))))
    } yield seqFPut
  }

  def findConfigByPartId(partId: String) = fetchFromCacheOrElse(partId)(delegate.findConfigByPartId, HttpPartConfigCacheKey(_))

  def findParamById(id: Long) = delegate.findParamById(id)

  def findAllCacheGroups() = delegate.findAllCacheGroups()

  def findCacheGroupByName(name: String) = fetchFromCacheOrElse(name)(delegate.findCacheGroupByName, CacheGroupCacheKey(_))

  def findAllCacheGroupsByName(names: String*) = delegate.findAllCacheGroupsByName(names: _*)

  /**
   * Generic Cache-decorated fetch
   *
   * Does a fetch from cache (fetch1), which returns a {{{Future[Option[Option[B]]]}}}.
   * If that Future contains a None (cache miss), we try to do a fetch using the delegate (fetch2)
   * and cache the result of that Future[Option[B]], and return the result of
   * fetch2 immediately. If the result of fetch1 is __NOT__ None (Some(Option[B])),
   * then we return Option[B] piece immediately inside a Future.successful().
   *
   * All of the above logic is followed by a recoverWith that will try a fetch via the delegate
   * if the any of it fails while executing asynchronously. There is a chance that inside the
   * cache-fetch miss, fetch2 finishes with an error and we end up trying to do another fetch from
   * the database but that is hard to side-step (and might not be such a bad thing)
   */
  private def fetchFromCacheOrElse[A, B](identifier: A)(notFromCacheFind: A => Future[Option[B]], cacheKey: A => CacheKey): Future[Option[B]] = {
    cache.get[Option[B]](cacheKey(identifier)).flatMap {
      _.fold {
        val fMaybeDBConfig = notFromCacheFind(identifier)
        fMaybeDBConfig.foreach(put(cacheKey(identifier), _))
        fMaybeDBConfig
      }(Future.successful)
    }.recoverWith {
      case NonFatal(e) =>
        Option(e.getCause).fold {
          LTSVLogger.error(e, "Cache retrieve this identifier" -> identifier.toString)
        } {
          case cause: shade.TimeoutException => LTSVLogger.warn("Cache retrieve timed out for this identifier" -> identifier.toString)
          case cause => LTSVLogger.error(cause, "Cache retrieve this identifier" -> identifier.toString)
        }

        notFromCacheFind(identifier)
    }
  }

  // ------ The below methods are NOT cached ------

  // this operation is not cached. only use in admin screen
  // it forces the cache puts to happen before completion to avoid discrepancies between screens
  def findAllConfigs() = delegate.findAllConfigs().flatMap {
    cSeq =>
      val futures = Future.sequence(cSeq.map { config =>
        {
          put(HttpPartConfigCacheKey(config.partId), Some(config)).map {
            finished => config
          }
        }
      })
      // Shutdown any HTTP clients for partIds that no longer exist (e.g due to renaming/deletion of configs)
      httpClientPool.cleanObsolete(cSeq.map(p => Symbol(p.partId)).toSet)
      futures
  }

  // not cached
  def findThreadPoolConfigById(id: Long) = delegate.findThreadPoolConfigById(id)

  // not cached
  def findAllThreadPoolConfigs() = delegate.findAllThreadPoolConfigs()

}
