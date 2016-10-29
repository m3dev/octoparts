package com.m3.octoparts.cache.memcached

import com.m3.octoparts.cache._
import com.m3.octoparts.cache.directive.CacheDirective
import com.m3.octoparts.cache.key.{ PartCacheKey, VersionCacheKey }
import com.m3.octoparts.cache.versioning._
import com.m3.octoparts.model.PartResponse
import com.twitter.zipkin.gen.Span
import shade.memcached.Codec

import scala.concurrent.duration.Duration
import scala.concurrent.{ ExecutionContext, Future }

class MemcachedCacheOps(
  cache: Cache,
  latestVersionCache: LatestVersionCache
)(implicit executionContext: ExecutionContext)
    extends CacheOps
    with TtlCalculator {

  import com.m3.octoparts.cache.versioning.LatestVersionCache.Version

  object PartCacheKeyFactory {
    // tries to make a part cache key using latest known version
    def tryApply(directive: CacheDirective)(implicit parentSpan: Span): Option[PartCacheKey] = {
      tryApply(directive, CombinedVersionLookup.knownVersions(directive))
    }

    def tryApply(directive: CacheDirective, internalVersions: Seq[Option[Version]]): Option[PartCacheKey] = {
      val aVersionIsUnknown = internalVersions.contains(None)
      if (aVersionIsUnknown) {
        None
      } else {
        Some(PartCacheKey(directive.partId, internalVersions.flatten, directive.paramValues))
      }
    }
  }

  object PartResponseCache {
    import play.api.libs.json.Json
    import shade.memcached.MemcachedCodecs.StringBinaryCodec
    import com.m3.octoparts.json.format.ReqResp._

    def pollPartResponse(cacheKey: PartCacheKey)(implicit parentSpan: Span): Future[Option[PartResponse]] = {
      val fMaybeString = cache.get[String](cacheKey)
      fMaybeString.map(maybeString => maybeString.map(Json.parse(_).as[PartResponse]))
    }

    def insertPartResponse(cacheKey: PartCacheKey, partResponse: PartResponse, ttl: Option[Duration])(implicit parentSpan: Span): Future[Unit] =
      cache.put[String](cacheKey, Json.toJson(partResponse).toString(), calculateTtl(partResponse.cacheControl, ttl))
  }

  object CombinedVersionLookup {
    def apply(directive: CacheDirective)(implicit parentSpan: Span): CombinedVersionLookup =
      CombinedVersionLookup(
        PartVersionCache(directive.partId).newLookup,
        directive.versionedParamKeys.map(ParamVersionCache(_).newLookup)
      )

    def knownVersions(directive: CacheDirective): Seq[Option[Version]] = {
      latestVersionCache.getPartVersion(directive.partId) +: directive.versionedParamKeys.map(latestVersionCache.getParamVersion)
    }
  }

  case class CombinedVersionLookup(
      partVersionLookup: VersionLookup[String], paramVersionLookups: Seq[VersionLookup[VersionedParamKey]]
  )(implicit parentSpan: Span) {

    lazy val all: Seq[VersionLookup[_]] = partVersionLookup +: paramVersionLookups

    def partId = partVersionLookup.versionCache.getId

    def internalVersions: Seq[Option[Version]] = all.map {
      _.internalVersion
    }

    // will all versions match ?
    def checkVersions: Future[Boolean] = {
      Future.sequence(all.map {
        _.willMatch
      }).map {
        bools =>
          !bools.contains(false)
      }
    }

    // updates the internal cache with versions found in external cache (authoritative)
    // it skips when a version was not found in the external cache
    lazy val updateLocal: Future[Seq[Unit]] = {
      Future.sequence(all.map {
        _.updateLocal()
      })
    }

    /**
     * inserts a new version in the external cache, for each missing one
     * @return true if something was written to the external cache
     */
    lazy val insertExternal: Future[Boolean] = {
      Future.sequence(all.map {
        _.insertExternal()
      }).map {
        bools => bools.contains(true)
      }
    }

    // both these tasks take care of synchronizing internal and external versions cache.
    def synchronizeVersions: Future[_] = Future.sequence(Seq(updateLocal, insertExternal))
  }

  sealed abstract class AbstractVersionCache[T <: java.io.Serializable](id: T) extends VersionCache[T](id) {
    private val versionCodec: Codec[Version] = Codec.LongBinaryCodec

    override def pollVersion(implicit parentSpan: Span): Future[Option[Version]] = cache.get[Version](VersionCacheKey(id))(versionCodec, parentSpan)

    override def doInsertExternal(version: Version)(implicit parentSpan: Span) = cache.put[Version](VersionCacheKey(id), version, Some(Duration.Inf))(versionCodec, parentSpan)
  }

  case class PartVersionCache(id: String) extends AbstractVersionCache(id) {
    override def knownVersion(implicit parentSpan: Span) = latestVersionCache.getPartVersion(id)

    override def updateVersion(version: Version)(implicit parentSpan: Span) = latestVersionCache.updatePartVersion(id, version)
  }

  case class ParamVersionCache(id: VersionedParamKey) extends AbstractVersionCache(id) {
    override def knownVersion(implicit parentSpan: Span) = latestVersionCache.getParamVersion(id)

    override def updateVersion(version: Version)(implicit parentSpan: Span) = latestVersionCache.updateParamVersion(id, version)
  }

  override def putIfAbsent(directive: CacheDirective)(f: => Future[PartResponse])(implicit parentSpan: Span): Future[PartResponse] = {

    // this asks all the versions we may need from internal and external caches
    // note: this apply() kicks of several cache polls
    val versionLookups = CombinedVersionLookup(directive)

    // for now, make a cache key using only internal versions.
    // this may result in a cache miss
    val maybeCacheKey = PartCacheKeyFactory.tryApply(directive, versionLookups.internalVersions)
    maybeCacheKey.fold {
      // some versions were missing and the cache key was not created
      onNotFound(versionLookups.synchronizeVersions, directive)(f)
    } {
      cacheKey =>
        PartResponseCache.pollPartResponse(cacheKey).flatMap {
          case Some(resp) =>
            // value was found in cache
            val checkedFuture = checkAndReturn(directive, resp, versionLookups)
            checkedFuture.flatMap {
              optResp =>
                optResp.fold {
                  // some versions were mismatched so the cached value was discarded
                  onNotFound(versionLookups.synchronizeVersions, directive)(f)
                } {
                  partResp =>
                    // All versions matched what we expected, so this is a valid cache hit
                    Future.successful(setRetrievedFromCacheFlag(partResp))
                }
            }

          case None =>
            // value was not found in cache
            onNotFound(versionLookups.synchronizeVersions, directive)(f)
        }
    }
  }

  override def increasePartVersion(partId: String)(implicit parentSpan: Span): Future[Unit] = PartVersionCache(partId).insertNewVersion()

  override def increaseParamVersion(vpk: VersionedParamKey)(implicit parentSpan: Span): Future[Unit] = ParamVersionCache(vpk).insertNewVersion()

  // assumes hashes do not collide : only versions are verified
  private def checkAndReturn(
    directive: CacheDirective, resp: PartResponse, versionLookups: CombinedVersionLookup
  )(implicit parentSpan: Span): Future[Option[PartResponse]] = {
    // really basic check to make sure we return a valid entry
    // e.g. has the right partId
    if (resp.partId == versionLookups.partId) {
      versionLookups.checkVersions.flatMap {
        versionWasOk =>
          if (versionWasOk) {
            // all green! return the response we already have
            Future.successful(Some(resp))
          } else {
            // some versions differ : synchronize the versions and then make another cache query, only if the cache had all versions set
            versionLookups.synchronizeVersions.flatMap {
              _ =>
                versionLookups.insertExternal.flatMap {
                  wasInserted =>
                    if (wasInserted) {
                      Future.successful(None)
                    } else {
                      // all version are there : the cacheKey should be able to be created
                      // however, the internal version cache may change anytime, so use a fold instead
                      PartCacheKeyFactory.tryApply(directive).fold {
                        Future.successful[Option[PartResponse]](None)
                      } {
                        cacheKey => PartResponseCache.pollPartResponse(cacheKey)
                      }
                    }
                }
            }
          }
      }
    } else {
      Future.successful(None)
    }
  }

  private def onNotFound(finishThisBefore: Future[_], directive: CacheDirective)(f: => Future[PartResponse])(implicit parentSpan: Span): Future[PartResponse] =
    finishThisBefore.flatMap {
      unused =>
        // not found. shall put it in when f succeeds (if ever)
        val resp: Future[PartResponse] = f
        resp.onSuccess {
          case partResponse =>
            saveLater(partResponse, directive)
        }
        resp
    }

  private def mayCache(partResponse: PartResponse): Boolean = !partResponse.cacheControl.noStore

  override def saveLater(partResponse: PartResponse, directive: CacheDirective)(implicit parentSpan: Span): Future[Unit] = {
    if (mayCache(partResponse)) {
      // renew the cache key
      // NOTE: At this point, the version futures have completed, so this is the latest known version data
      PartCacheKeyFactory.tryApply(directive).fold {
        // in case the internal cache was cleaned in-between
        Future.successful(())
      } {
        // store
        cacheKey => PartResponseCache.insertPartResponse(cacheKey, partResponse, directive.ttl)
      }
    } else {
      Future.successful(())
    }
  }

  private def setRetrievedFromCacheFlag(partResponse: PartResponse) = partResponse.copy(retrievedFromCache = true)

}
