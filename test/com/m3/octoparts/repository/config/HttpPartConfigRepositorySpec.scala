package com.m3.octoparts.repository.config

import java.nio.charset.StandardCharsets

import com.m3.octoparts.model.HttpMethod._
import com.m3.octoparts.model.config._
import com.m3.octoparts.support.db.DBSuite
import com.m3.octoparts.support.mocks.ConfigDataMocks
import org.joda.time.DateTime
import org.scalatest.{ Matchers, fixture }
import scalikejdbc.DBSession

import scala.concurrent.duration._

class HttpPartConfigRepositorySpec extends fixture.FunSpec with DBSuite with Matchers with ConfigDataMocks {

  val httpPartConfigMap: Seq[(Symbol, Any)] = Seq(
    'partId -> "whatUpDependency",
    'owner -> "someone@me.com",
    'uriToInterpolate -> "http://skysports.com/fooball",
    'method -> "get",
    'httpPoolSize -> 5,
    'httpConnectionTimeout -> 1.second.toMillis,
    'httpSocketTimeout -> 5.seconds.toMillis,
    'httpDefaultEncoding -> StandardCharsets.UTF_8.name()
  )

  val httpPartConfigMapWithDeprecation = httpPartConfigMap :+ ('deprecatedInFavourOf -> Some("theNewHotness"))

  // ---- Helper functions ----
  private def createCacheGroup(howMany: Int = 1)(implicit session: DBSession): Seq[CacheGroup] = {
    for (i <- 0 until howMany) yield {
      val id = CacheGroupRepository.createWithAttributes('name -> s"myLittleGroup $i")
      CacheGroupRepository.findById(id).get
    }
  }

  private def createPartConfig(implicit session: DBSession): HttpPartConfig = {
    val partId = HttpPartConfigRepository.save(mockHttpPartConfig)
    HttpPartConfigRepository.findById(partId).get
  }

  describe("creating a HttpPartConfig and reading it back") {

    it("should work when not passing Option values") {
      implicit session =>
        val id = HttpPartConfigRepository.createWithAttributes(httpPartConfigMap: _*)
        val config = HttpPartConfigRepository.findById(id).get
        config.partId should be("whatUpDependency")
        config.owner should be("someone@me.com")
        config.uriToInterpolate should be("http://skysports.com/fooball")
        config.method should be(Get)
        config.deprecatedInFavourOf should be(None)
        config.httpPoolSize should be(5)
        config.httpConnectionTimeout should be(1.second)
        config.httpSocketTimeout should be(5.seconds)
        config.httpDefaultEncoding should be(StandardCharsets.UTF_8)
    }

    it("should work when passing Option values") {
      implicit session =>
        val id = HttpPartConfigRepository.createWithAttributes(httpPartConfigMapWithDeprecation: _*)
        val config = HttpPartConfigRepository.findById(id).get
        config.partId should be("whatUpDependency")
        config.owner should be("someone@me.com")
        config.uriToInterpolate should be("http://skysports.com/fooball")
        config.method should be(Get)
        config.deprecatedInFavourOf should be(Some("theNewHotness"))
    }

  }

  describe("hasMany PartParam") {

    it("should be possible to retrieve the parameters of a HttpPartConfig with the .joins method") {
      implicit session =>
        val partId = HttpPartConfigRepository.createWithAttributes(httpPartConfigMapWithDeprecation: _*)
        PartParamRepository.createWithAttributes(
          'httpPartConfigId -> partId,
          'required -> true,
          'paramType -> "header",
          'outputName -> "myParam"
        )
        val config = HttpPartConfigRepository.joins(HttpPartConfigRepository.paramsRef).findById(partId).get
        val partParams = config.parameters
        val partParam = partParams.head
        partParam.required should be(true)
        partParam.paramType should be(ParamType.Header)
        partParam.outputName should be("myParam")
    }

    describe("saving CacheGroup on a PartParam child model") {
      describe("when passing CacheGroup as part of a new insert") {

        /*
          Again, note that we need to load PartParam separately because eager loading a hasManyThrough model's
          hasManyThrough does NOT seem to work
         */
        it("should allow me retrieve the PartParam and get back the CacheGroups") { implicit session =>
          val cacheGroups = createCacheGroup(3).toSet
          val partParam = mockPartParam.copy(id = None, cacheGroups = cacheGroups)
          val partId = HttpPartConfigRepository.save(mockHttpPartConfig.copy(parameters = Set(partParam)))
          val partParamCacheGroupIdsRetrieved = for {
            param <- PartParamRepository.findByPartId(partId)
            cacheGroup <- param.cacheGroups
          } yield cacheGroup.id
          partParamCacheGroupIdsRetrieved should be(cacheGroups.toSeq.map(_.id))
        }

        it("should add the PartParam to the list of partParams on the CacheGroups") { implicit session =>
          val cacheGroups = createCacheGroup(3).toSet
          val partParam = mockPartParam.copy(id = None, cacheGroups = cacheGroups)
          HttpPartConfigRepository.save(mockHttpPartConfig.copy(parameters = Set(partParam)))
          val retrievedCacheGroups = cacheGroups.map(cG => CacheGroupRepository.withChildren.findById(cG.id.get).get)
          retrievedCacheGroups.foreach(_.partParams.map(_.outputName) should contain(partParam.outputName))
        }

      }

      describe("when passing a CacheGroup id as part of an update") {

        it("should allow me retrieve the HttpPartConfig and get back the CacheGroup within each PartParam") { implicit session =>
          val cacheGroups = createCacheGroup(3).toSet
          val part = createPartConfig
          val partParam = mockPartParam.copy(id = None, outputName = "param1", cacheGroups = cacheGroups, httpPartConfigId = part.id)
          HttpPartConfigRepository.save(part.copy(parameters = Set(partParam)))
          val partParamCacheGroupIdsRetrieved = for {
            param <- PartParamRepository.findByPartId(part.id.get)
            cacheGroup <- param.cacheGroups
          } yield cacheGroup.id
          partParamCacheGroupIdsRetrieved should be(cacheGroups.map(_.id).toSeq)
        }

        it("should add the PartParam to the list of partParams on the CacheGroup") { implicit session =>
          val cacheGroups = createCacheGroup(3).toSet
          val part = createPartConfig
          val partParam = mockPartParam.copy(id = None, outputName = "param2", cacheGroups = cacheGroups, httpPartConfigId = part.id)
          HttpPartConfigRepository.save(part.copy(parameters = Set(partParam)))
          val retrievedCacheGroups = cacheGroups.map(cG => CacheGroupRepository.withChildren.findById(cG.id.get).get)
          retrievedCacheGroups.foreach(_.partParams.map(_.outputName) should contain(partParam.outputName))
        }

      }
    }

  }

  describe("hasOne HystrixConfig") {

    def setup(implicit session: DBSession): Long = {
      val partId = HttpPartConfigRepository.createWithAttributes(httpPartConfigMapWithDeprecation: _*)
      PartParamRepository.createWithAttributes(
        'httpPartConfigId -> partId,
        'required -> true,
        'paramType -> "header",
        'outputName -> "myParam"
      )
      val threadPoolConfigId = ThreadPoolConfigRepository.createWithAttributes(
        'threadPoolKey -> "swimmingpool",
        'coreSize -> 10
      )
      HystrixConfigRepository.createWithAttributes(
        'httpPartConfigId -> partId,
        'threadPoolConfigId -> threadPoolConfigId,
        'commandKey -> "myWish",
        'commandGroupKey -> "isYourCommand",
        'timeoutInMs -> 3.seconds.toMillis
      )
      partId
    }

    it("should be possible to retrieve the HystrixConfig of a HttpPartConfig with the .joins method") {
      implicit session =>
        val partId = setup(session)
        val config = HttpPartConfigRepository.joins(HttpPartConfigRepository.hystrixConfigRef).findById(partId).get
        val hystrixConfig = config.hystrixConfig.get
        hystrixConfig.timeoutInMs should be(3.seconds)
        hystrixConfig.commandKey should be("myWish")
        hystrixConfig.commandGroupKey should be("isYourCommand")
    }

    it("should be possible to retrieve the HystrixConfig of a HttpPartConfig, along with it's ThreadPoolConfig, using the .includes method") {
      implicit session =>
        val partId = setup(session)
        val config = HttpPartConfigRepository.includes(HttpPartConfigRepository.hystrixConfigRef).findById(partId).get
        val hystrixConfig = config.hystrixConfig.get
        val Some(threadPoolConfig) = hystrixConfig.threadPoolConfig // will fail if not eager loaded
        threadPoolConfig.threadPoolKey should be("swimmingpool")
        threadPoolConfig.coreSize should be(10)
    }

  }

  describe(".save") {

    lazy val parameters = Seq(
      PartParam(required = true, versioned = false, paramType = ParamType.Cookie, outputName = "myCookie", createdAt = DateTime.now, updatedAt = DateTime.now),
      PartParam(required = false, versioned = false, paramType = ParamType.Header, outputName = "myHeader", createdAt = DateTime.now, updatedAt = DateTime.now)
    )

    lazy val threadPool = ThreadPoolConfig(threadPoolKey = "myThreadPool", coreSize = 5, createdAt = DateTime.now, updatedAt = DateTime.now)

    /*
      Inside a method that takes a session because we need to refer to a valid ThreadPoolConfig id when
      Instantiating a HystrixConfig.
    */
    def hystrixConfig(implicit session: DBSession): HystrixConfig =
      HystrixConfig(
        threadPoolConfigId = Some(ThreadPoolConfigRepository.save(threadPool)),
        commandKey = "myCommand",
        commandGroupKey = "myCommandGroup",
        createdAt = DateTime.now,
        updatedAt = DateTime.now)

    /**
     * Compares two HttpPartConfigs.
     *
     * Necessary because we have audit fields and ids that may not be set on insert
     *
     * @param expected
     * @param observed
     */
    def compare(expected: HttpPartConfig, observed: HttpPartConfig) = {
      val observedHystrixConfig = observed.hystrixConfigItem
      val observedThreadPoolConfig = observedHystrixConfig.threadPoolConfigItem
      val observedConfigParams = observed.parameters.toSeq.sortWith(_.outputName < _.outputName)

      val expectedHystrixConfig = expected.hystrixConfigItem

      for {
        (observed, expected) <- observedConfigParams.sortBy(_.inputName).zip(expected.parameters.toSeq.sortBy(_.inputName))
      } {
        observed.required should be(expected.required)
        observed.paramType should be(expected.paramType)
        observed.outputName should be(expected.outputName)
      }

      observed.partId should be(expected.partId)
      observed.owner should be(expected.owner)
      observed.description should be(expected.description)
      observed.uriToInterpolate should be(expected.uriToInterpolate)
      observed.method should be(expected.method)

      observedHystrixConfig.commandKey should be(expectedHystrixConfig.commandKey)
      observedHystrixConfig.commandGroupKey should be(expectedHystrixConfig.commandGroupKey)
      observedHystrixConfig.timeoutInMs should be(expectedHystrixConfig.timeoutInMs)

      observedThreadPoolConfig.threadPoolKey should be(threadPool.threadPoolKey)
      observedThreadPoolConfig.coreSize should be(threadPool.coreSize)
    }

    describe("to insert") {

      it("should successfully persist all attributes, including the children models") {
        implicit session =>
          val insertedHystrixConfig = hystrixConfig
          val insertedConfig = HttpPartConfig(
            partId = "testingWithModels",
            owner = "pwner",
            description = Some("just trying this out"),
            uriToInterpolate = "http://scala-lang.org",
            method = Get,
            httpPoolSize = 5,
            httpConnectionTimeout = 1.second,
            httpSocketTimeout = 5.seconds,
            httpDefaultEncoding = StandardCharsets.US_ASCII,
            parameters = parameters.toSet,
            hystrixConfig = Some(insertedHystrixConfig),
            cacheTtl = Some(30.seconds),
            alertMailsEnabled = true,
            alertAbsoluteThreshold = Some(123),
            alertPercentThreshold = Some(50),
            alertInterval = 5.minutes,
            alertMailRecipients = Some("c-birchall@m3.com"),
            createdAt = DateTime.now,
            updatedAt = DateTime.now
          )
          val savedId = HttpPartConfigRepository.save(insertedConfig)
          // If any of these .get fail, then we're already in trouble so stop judging
          val retrievedTopConfig = HttpPartConfigRepository.includes(HttpPartConfigRepository.hystrixConfigRef).findById(savedId).get
          compare(insertedConfig, retrievedTopConfig)
      }

    }

    describe("to save an existing record") {

      def preInsertedConfig(implicit session: DBSession): HttpPartConfig = {
        val insertedHystrixConfig = hystrixConfig
        val insertedConfig = HttpPartConfig(
          partId = "testingWithModels",
          owner = "pwner",
          description = Some("just trying this out"),
          uriToInterpolate = "http://scala-lang.org",
          method = Get,
          httpPoolSize = 5,
          httpConnectionTimeout = 1.second,
          httpSocketTimeout = 5.seconds,
          httpDefaultEncoding = StandardCharsets.US_ASCII,
          parameters = parameters.toSet,
          hystrixConfig = Some(insertedHystrixConfig),
          cacheTtl = Some(10.minutes),
          alertMailsEnabled = true,
          alertAbsoluteThreshold = Some(123),
          alertPercentThreshold = Some(50),
          alertInterval = 5.minutes,
          alertMailRecipients = Some("v-pericart@m3.com"),
          createdAt = DateTime.now,
          updatedAt = DateTime.now
        )
        val savedId = HttpPartConfigRepository.save(insertedConfig)
        HttpPartConfigRepository.includes(HttpPartConfigRepository.hystrixConfigRef).findById(savedId).get
      }

      it("should successfully persist all changes when one field is updated") {
        implicit session: DBSession =>
          val oldConfig = preInsertedConfig
          val updatedConfig = oldConfig.copy(
            cacheTtl = Some(99.seconds)
          )
          HttpPartConfigRepository.save(updatedConfig)
          val newlyRetrievedConfig = HttpPartConfigRepository.includes(HttpPartConfigRepository.hystrixConfigRef).findById(oldConfig.id.get).get
          compare(updatedConfig, newlyRetrievedConfig)
      }

      it("should successfully persist all changes, including ones to children models") {
        implicit session: DBSession =>
          val oldConfig = preInsertedConfig
          val updatedParams = oldConfig.parameters.map(p => p.copy(required = !p.required, outputName = p.outputName + "_new"))
          val updatedHystrixConfig = oldConfig.hystrixConfig.map {
            c =>
              c.copy(
                commandKey = c.commandKey + "_new",
                commandGroupKey = c.commandGroupKey + "_new",
                timeoutInMs = 999.milliseconds
              )
          }
          val updatedConfig = oldConfig.copy(
            partId = "northernFront",
            owner = "newManagement",
            uriToInterpolate = "http://beachape.com",
            description = Some("Nothing but the newest"),
            parameters = updatedParams,
            hystrixConfig = updatedHystrixConfig
          )
          HttpPartConfigRepository.save(updatedConfig)
          val newlyRetrievedConfig = HttpPartConfigRepository.includes(HttpPartConfigRepository.hystrixConfigRef).findById(oldConfig.id.get).get
          compare(updatedConfig, newlyRetrievedConfig)
      }
    }

  }

  describe("hasMany CacheGroups") {

    describe("when passing CacheGroup as part of a new insert") {

      it("should allow me retrieve the HttpPartConfig and get back the CacheGroup") { implicit session =>
        val cacheGroups = createCacheGroup(3).toSet
        val partId = HttpPartConfigRepository.save(mockHttpPartConfig.copy(cacheGroups = cacheGroups))
        val retrievedPart = HttpPartConfigRepository.findById(partId).get
        /*
         $1 billion question: Why does the above (using .byDefault) work, but the below, using .includes
         break by not finding any records?

         Note: You will need to go remove the .byDefaults in HttpPartConfig

         val retrievedPart = HttpPartConfig.includes(
           HttpPartConfig.cacheGroupsRef, HttpPartConfig.hystrixConfigRef, HttpPartConfig.paramsRef
         ).findById(partId).get
         */
        retrievedPart.cacheGroups.map(_.id) should be(cacheGroups.map(_.id))
      }

      it("should add the HttpPartConfig to the list of httpPartConfigs on the CacheGroup") { implicit session =>
        val cacheGroups = createCacheGroup(2).toSet
        val partId = HttpPartConfigRepository.save(mockHttpPartConfig.copy(cacheGroups = cacheGroups))
        val retrievedCacheGroups = cacheGroups.map(cG => CacheGroupRepository.withChildren.findById(cG.id.get).get)
        retrievedCacheGroups.foreach(_.httpPartConfigs.map(_.id) should contain(Some(partId)))
      }

    }

    describe("when passing a CacheGroup id as part of an update") {

      it("should allow me retrieve the HttpPartConfig and get back the CacheGroup") { implicit session =>
        val cacheGroups = createCacheGroup(3).toSet
        val part = createPartConfig
        HttpPartConfigRepository.save(part.copy(cacheGroups = cacheGroups))
        val retrievedPart = HttpPartConfigRepository.findById(part.id.get)
        retrievedPart.get.cacheGroups.map(_.id) should be(cacheGroups.map(_.id))
      }

      it("should add the HttpPartConfig to the list of httpPartConfigs on the CacheGroup") { implicit session =>
        val cacheGroups = createCacheGroup(3).toSet
        val part = createPartConfig
        HttpPartConfigRepository.save(part.copy(cacheGroups = cacheGroups))
        val retrievedCacheGroups = cacheGroups.map(cG => CacheGroupRepository.withChildren.findById(cG.id.get).get)
        retrievedCacheGroups.foreach(_.httpPartConfigs.map(_.id) should contain(Some(part.id.get)))
      }

    }

  }
}
