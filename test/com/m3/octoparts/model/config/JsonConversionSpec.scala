package com.m3.octoparts.model.config

import java.nio.charset.{ Charset => JavaCharset }
import java.util.concurrent.TimeUnit

import com.m3.octoparts.model.HttpMethod
import org.scalacheck.{ Arbitrary, Gen }
import org.scalatest._
import org.scalatest.prop.{ Checkers, GeneratorDrivenPropertyChecks }

import scala.collection.SortedSet
import scala.collection.convert.Wrappers._
import scala.concurrent.duration.FiniteDuration

class JsonConversionSpec extends FunSpec with Matchers with Checkers with GeneratorDrivenPropertyChecks {

  private implicit val arbCacheGroup: Arbitrary[json.CacheGroup] = Arbitrary {
    for {
      name <- Arbitrary.arbString.arbitrary
      owner <- Arbitrary.arbString.arbitrary
      description <- Arbitrary.arbString.arbitrary
    } yield {
      json.CacheGroup(name = name, owner = owner, description = description)
    }
  }

  private implicit val arbThreadPoolConfig: Arbitrary[json.ThreadPoolConfig] = Arbitrary {
    for {
      threadPoolKey <- Arbitrary.arbString.arbitrary
      coreSize <- Gen.chooseNum(0, Int.MaxValue)
      queueSize <- Gen.chooseNum(0, Int.MaxValue)
    } yield {
      json.ThreadPoolConfig(threadPoolKey = threadPoolKey, coreSize = coreSize, queueSize = queueSize)
    }
  }

  private val genShortDuration: Gen[FiniteDuration] = {
    for {
      length <- Gen.chooseNum(0L, TimeUnit.DAYS.toMillis(1L))
    } yield {
      FiniteDuration(length, TimeUnit.MILLISECONDS)
    }
  }

  private implicit val arbHystrixConfig: Arbitrary[json.HystrixConfig] = Arbitrary {
    for {
      timeout <- genShortDuration
      threadPoolConfig <- arbThreadPoolConfig.arbitrary
      commandKey <- Arbitrary.arbString.arbitrary
      commandGroupKey <- Arbitrary.arbString.arbitrary
      localContentsAsFallback <- Arbitrary.arbBool.arbitrary
    } yield {
      json.HystrixConfig(
        timeout = timeout,
        threadPoolConfig = threadPoolConfig,
        commandKey = commandKey,
        commandGroupKey = commandGroupKey,
        localContentsAsFallback = localContentsAsFallback
      )
    }
  }

  private implicit val arbPartParam: Arbitrary[json.PartParam] = Arbitrary {
    for {
      required <- Arbitrary.arbBool.arbitrary
      versioned <- Arbitrary.arbBool.arbitrary
      paramType <- Gen.oneOf(ParamType.values.toSeq)
      outputName <- Arbitrary.arbString.arbitrary
      inputNameOverride <- Gen.option(Arbitrary.arbString.arbitrary)
      description <- Gen.option(Arbitrary.arbString.arbitrary)
      cacheGroups <- Gen.containerOf[SortedSet, json.CacheGroup](arbCacheGroup.arbitrary)
    } yield {
      json.PartParam(
        required = required,
        versioned = versioned,
        paramType = paramType,
        outputName = outputName,
        description = description,
        inputNameOverride = inputNameOverride,
        cacheGroups = cacheGroups.toSet)
    }
  }

  private implicit val arbAlertMailSettings: Arbitrary[json.AlertMailSettings] = Arbitrary {
    for {

      alertMailsEnabled <- Arbitrary.arbBool.arbitrary
      alertAbsoluteThreshold <- Gen.option(Gen.choose(0, Int.MaxValue))
      alertPercentThreshold <- Gen.option(Gen.choose(0.0, 100.0))
      alertInterval <- genShortDuration
      alertMailRecipients <- Gen.option(Gen.identifier)
    } yield {
      json.AlertMailSettings(
        alertMailsEnabled = alertMailsEnabled,
        alertAbsoluteThreshold = alertAbsoluteThreshold,
        alertPercentThreshold = alertPercentThreshold,
        alertInterval = alertInterval,
        alertMailRecipients = alertMailRecipients
      )
    }
  }

  private implicit val arbHttpPartConfig: Arbitrary[json.HttpPartConfig] = Arbitrary {
    for {
      partId <- Arbitrary.arbString.arbitrary
      owner <- Arbitrary.arbString.arbitrary
      uriToInterpolate <- Arbitrary.arbString.arbitrary
      description <- Gen.option(Arbitrary.arbString.arbitrary)
      method <- Gen.oneOf(HttpMethod.values.toSeq)
      hystrixConfig <- arbHystrixConfig.arbitrary
      additionalValidStatuses <- Gen.containerOf[Set, Int](Gen.choose(400, 599))
      httpPoolSize <- Gen.chooseNum(1, Int.MaxValue)
      httpConnectionTimeout <- genShortDuration
      httpSocketTimeout <- genShortDuration
      httpDefaultEncoding <- Gen.oneOf(JCollectionWrapper(JavaCharset.availableCharsets().values()).toSeq)
      parameters <- Gen.containerOf[SortedSet, json.PartParam](arbPartParam.arbitrary)
      deprecatedInFavourOf <- Gen.option(Arbitrary.arbString.arbitrary)
      cacheGroups <- Gen.containerOf[SortedSet, json.CacheGroup](arbCacheGroup.arbitrary)
      cacheTtl <- Gen.option(genShortDuration)
      alertMailSettings <- arbAlertMailSettings.arbitrary
      httpProxy <- Gen.option(Arbitrary.arbString.arbitrary)

    } yield {
      json.HttpPartConfig(
        partId = partId,
        owner = owner,
        uriToInterpolate = uriToInterpolate,
        description = description,
        method = method,
        hystrixConfig = hystrixConfig,
        additionalValidStatuses = additionalValidStatuses,
        httpPoolSize = httpPoolSize,
        httpConnectionTimeout = httpConnectionTimeout,
        httpSocketTimeout = httpSocketTimeout,
        httpDefaultEncoding = httpDefaultEncoding,
        httpProxy = httpProxy,
        parameters = parameters.toSet,
        deprecatedInFavourOf = deprecatedInFavourOf,
        cacheGroups = cacheGroups.toSet,
        cacheTtl = cacheTtl,
        alertMailSettings = alertMailSettings
      )
    }
  }

  it("ThreadPoolConfig should convert and back") {
    forAll {
      jtpc: json.ThreadPoolConfig =>
        val tpc = ThreadPoolConfig.fromJsonModel(jtpc)
        tpc.threadPoolKey should be(jtpc.threadPoolKey)
        tpc.coreSize should be(jtpc.coreSize)
        tpc.queueSize should be(jtpc.queueSize)
        ThreadPoolConfig.toJsonModel(tpc) shouldBe jtpc
    }
  }

  it("HystrixConfig should convert and back") {
    forAll {
      jhc: json.HystrixConfig =>
        val hc = HystrixConfig.fromJsonModel(jhc)
        hc.timeout should be(jhc.timeout)
        hc.commandKey should be(jhc.commandKey)
        hc.commandGroupKey should be(jhc.commandGroupKey)
        HystrixConfig.toJsonModel(hc) shouldBe jhc
    }
  }

  it("CacheGroup should convert and back") {
    forAll {
      jcg: json.CacheGroup =>
        val cg = CacheGroup.fromJsonModel(jcg)
        cg.name should be(jcg.name)
        cg.description should be(jcg.description)
        cg.owner should be(jcg.owner)
        CacheGroup.toJsonModel(cg) should be(jcg)
    }
  }

  it("PartParam should convert and back") {
    forAll {
      jpp: json.PartParam =>
        val pp = PartParam.fromJsonModel(jpp)
        pp.required should be(jpp.required)
        pp.versioned should be(jpp.versioned)
        pp.paramType should be(jpp.paramType)
        pp.outputName should be(jpp.outputName)
        pp.inputNameOverride should be(jpp.inputNameOverride)
        pp.cacheGroups.map(CacheGroup.toJsonModel) should be(jpp.cacheGroups.to[SortedSet])
        PartParam.toJsonModel(pp) should be(jpp)
    }
  }

  it("HttpPartConfig should convert and back") {
    forAll {
      jhpc: json.HttpPartConfig =>
        val hpc = HttpPartConfig.fromJsonModel(jhpc)
        hpc.partId should be(jhpc.partId)
        hpc.owner should be(jhpc.owner)
        hpc.uriToInterpolate should be(jhpc.uriToInterpolate)
        hpc.description should be(jhpc.description)
        hpc.method should be(jhpc.method)
        val ejhc = jhpc.hystrixConfig
        hpc.hystrixConfig.map(HystrixConfig.toJsonModel) should be(Some(ejhc))
        hpc.additionalValidStatuses should equal(jhpc.additionalValidStatuses.to[SortedSet])
        hpc.parameters.map(PartParam.toJsonModel) should be(jhpc.parameters.to[SortedSet])
        hpc.deprecatedInFavourOf should be(jhpc.deprecatedInFavourOf)
        hpc.cacheGroups.map(CacheGroup.toJsonModel) should be(jhpc.cacheGroups.to[SortedSet])
        hpc.cacheTtl should be(jhpc.cacheTtl)
        hpc.alertMailsEnabled should be(jhpc.alertMailSettings.alertMailsEnabled)
        hpc.alertAbsoluteThreshold should be(jhpc.alertMailSettings.alertAbsoluteThreshold)
        hpc.alertPercentThreshold should be(jhpc.alertMailSettings.alertPercentThreshold)
        hpc.alertInterval should be(jhpc.alertMailSettings.alertInterval)
        hpc.alertMailRecipients should be(jhpc.alertMailSettings.alertMailRecipients)
        HttpPartConfig.toJsonModel(hpc) shouldBe jhpc
    }
  }
}
