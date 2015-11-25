import sbt._
import sbt.Keys._
import play.sbt.Play.autoImport._

object Dependencies {

  val resolverSettings = {
    // Use in-house Maven repo before other remote repos if env var is set
    resolvers ++= Seq(Resolver.defaultLocal) ++ sys.env.get("INHOUSE_MAVEN_REPO").map("Inhouse".at) ++ Seq(
      Resolver.typesafeRepo("releases"),
      Resolver.sonatypeRepo("releases"),
      Resolver.sonatypeRepo("snapshots")
      )
  }

  val thePlayVersion = play.core.PlayVersion.current
  val slf4jVersion = "1.7.10"
  val hystrixVersion = "1.4.1"
  // http-client 4.4 has an unsolved issue which affects us critically: https://issues.apache.org/jira/browse/HTTPCLIENT-1609
  // Stay on 4.3.x until this is fixed.
  val httpClientVersion = "4.3.6"
  val scalikejdbcVersion = "2.3.0"
  val swaggerVersion = "1.3.12"
  val jacksonVersion = "2.5.1"
  val macwireVersion = "2.1.0"

  // Logging
  val logbackClassic      = "ch.qos.logback"            % "logback-classic"               % "1.1.3"
  val slf4jApi            = "org.slf4j"                 % "slf4j-api"                     % slf4jVersion
  val jclOverSlf4j        = "org.slf4j"                 % "jcl-over-slf4j"                % slf4jVersion
  val log4jOverSlf4j      = "org.slf4j"                 % "log4j-over-slf4j"              % slf4jVersion
  val julToSlf4j          = "org.slf4j"                 % "jul-to-slf4j"                  % slf4jVersion
  val ravenLogback        = "net.kencochrane.raven"     % "raven-logback"                 % "6.0.0"   % Runtime
  val janino              = "org.codehaus.janino"       % "janino"                        % "2.7.8"
  val ltsvLogger          = "com.beachape"              %% "ltsv-logger"                  % "0.0.9"

  // Hystrix
  val hystrixCore         = "com.netflix.hystrix"       % "hystrix-core"                  % hystrixVersion
  val hystrixStream       = "com.netflix.hystrix"       % "hystrix-metrics-event-stream"  % hystrixVersion
  val rxJavaScala         = "io.reactivex"              %% "rxscala"                      % "0.24.0" // compatible with the rxjava (1.0.7) used in hystrix-core. Check again if you change.

  // HTTP clients
  val asyncHttpClient     = "com.ning"                  % "async-http-client"             % "1.9.11" // not upgrading because play-ws uses this version
  val httpClient          = "org.apache.httpcomponents" % "httpclient"                    % httpClientVersion
  val httpClientCache     = "org.apache.httpcomponents" % "httpclient-cache"              % httpClientVersion
  val metricsHttpClient   = "io.dropwizard.metrics"     % "metrics-httpclient"            % "3.1.1"

  // DB
  val postgres            = "org.postgresql"            % "postgresql"                    % "9.4-1201-jdbc41"   % Runtime
  val skinnyOrm           = "org.skinny-framework"      %% "skinny-orm"                   % "2.0.1"
  val scalikeJdbc         = "org.scalikejdbc"           %% "scalikejdbc"                  % scalikejdbcVersion
  val scalikeJdbcConfig   = "org.scalikejdbc"           %% "scalikejdbc-config"           % scalikejdbcVersion
  val scalikeJdbcPlay     = "org.scalikejdbc"           %% "scalikejdbc-play-initializer" % "2.4.3"
  val dbcp2               = "org.apache.commons"        % "commons-dbcp2"                 % "2.1"

  // Memcached
  val shade               = "com.bionicspirit"          %% "shade"                        % "1.6.0"
  val spyMemcached        = "net.spy"                   % "spymemcached"                  % "2.11.6"

  // Play plugins
  val playFlyway          = "org.flywaydb"              %% "flyway-play"                  % "2.2.0"
  val providedPlay        = "com.typesafe.play"         %% "play"                         % thePlayVersion      % Provided

  // DI
  val macwireMacros       = "com.softwaremill.macwire"  %% "macros"                       % macwireVersion

  // Swagger
  val swaggerPlay24       = "pl.matisoft"               %% "swagger-play24"               % "1.4" // Replace with Official version once 1.3.13 hits
  val swaggerAnnotations  = "com.wordnik"               % "swagger-annotations"           % swaggerVersion

  // Jackson
  val jacksonCore         = "com.fasterxml.jackson.core"   % "jackson-core"               % jacksonVersion
  val jacksonScala        = "com.fasterxml.jackson.module" %% "jackson-module-scala"      % jacksonVersion
  val jacksonDatabind     = "com.fasterxml.jackson.core"   % "jackson-databind"           % jacksonVersion

  // Test
  val playTest            = "com.typesafe.play"         %% "play-test"                    % thePlayVersion      % Test
  val scalatest           = "org.scalatest"             %% "scalatest"                    % "2.2.4"             % Test
  val scalatestPlay       = "org.scalatestplus"         %% "play"                         % "1.4.0-M3"          % Test
  val scalacheck          = "org.scalacheck"            %% "scalacheck"                   % "1.12.2"            % Test
  val groovy              = "org.codehaus.groovy"       % "groovy"                        % "2.4.1"             % Test
  val scalikeJdbcTest     = "org.scalikejdbc"           %% "scalikejdbc-test"             % scalikejdbcVersion  % Test
  val mockitoCore         =  "org.mockito"              % "mockito-core"                  % "1.10.19"           % Test

  // Misc utils
  val commonsValidator    = "commons-validator"         % "commons-validator"             % "1.4.1"             % Runtime
  val guava               = "com.google.guava"          % "guava"                         % "18.0"
  val jta                 = "javax.transaction"         % "jta"                           % "1.1"
  val scalaUri            = "com.netaporter"            %% "scala-uri"                    % "0.4.6"
  val findbugs            = "com.google.code.findbugs"  % "jsr305"                        % "3.0.0"

  val kenshoo             = "com.kenshoo"               %% "metrics-play"                 % "2.4.0_0.4.1"

  val zipkinFutures       = "com.beachape"              %% "zipkin-futures-play"          % "0.2.0"

  val withoutExcluded = { (m: ModuleID) =>
    m.excludeAll(
      ExclusionRule(organization = "spy", name = "spymemcached"), // spymemcached's org changed from spy to net.spy
      ExclusionRule(organization = "org.slf4j", name = "slf4j-log4j12"),
      ExclusionRule(organization = "org.slf4j", name = "slf4j-jdk14"),
      ExclusionRule(organization = "org.slf4j", name = "slf4j-jcl"),
      ExclusionRule(organization = "org.slf4j", name = "slf4j-nop"),
      ExclusionRule(organization = "org.slf4j", name = "slf4j-simple"))}

  val rootDependencies = Seq(
    // Logging
    logbackClassic,
    slf4jApi,
    jclOverSlf4j,
    log4jOverSlf4j,
    julToSlf4j,
    ravenLogback,
    janino,
    ltsvLogger,

    //Hystrix
    hystrixCore,
    hystrixStream,
    rxJavaScala,

    // Apache HTTP client
    httpClient,
    httpClientCache,
    metricsHttpClient,

    // DB
    postgres,
    skinnyOrm,
    scalikeJdbc,
    scalikeJdbcConfig,
    scalikeJdbcPlay,
    dbcp2,

    // Memcached
    shade,
    spyMemcached,

    // Zipkin
    zipkinFutures,

    // Misc utils
    kenshoo,
    commonsValidator,
    jta,
    scalaUri,

    // DI
    macwireMacros,

    // Play plugins
    playFlyway,
    swaggerPlay24,

    // Test
    playTest,
    scalatest,
    scalatestPlay,
    scalacheck,
    groovy,
    scalikeJdbcTest,
    mockitoCore,
    ws
  ).map(withoutExcluded)

  val playScalatestDependencies = Seq(
    scalatest,
    scalatestPlay,
    mockitoCore
  )

  val authPluginDependencies = Seq(
    providedPlay,
    ltsvLogger
  )

  val modelsDependencies = Seq(
    swaggerAnnotations intransitive(),
    jacksonCore intransitive(),
    jacksonScala intransitive()
  )

  val javaClientDependncies = Seq(
    findbugs intransitive(),
    guava,
    slf4jApi,
    asyncHttpClient,
    jacksonCore,
    jacksonScala,
    jacksonDatabind,
    logbackClassic % Test,
    jclOverSlf4j intransitive(),
    log4jOverSlf4j intransitive(),
    julToSlf4j intransitive(),
    scalatest
  )

  val playJsonFormatsDependencies = playScalatestDependencies :+ json

  val scalaWsClientDependencies = playScalatestDependencies :+ ws
}