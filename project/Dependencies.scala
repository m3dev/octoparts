import sbt._
import sbt.Keys._
import play.Play.autoImport._

object Dependencies {

  val resolverSettings = {
    // Use in-house Maven repo before other remote repos if env var is set
    resolvers ++= Seq(Resolver.defaultLocal) ++ sys.env.get("INHOUSE_MAVEN_REPO").map("Inhouse".at) ++ Seq(
      Resolver.typesafeRepo("releases"),
      Resolver.sonatypeRepo("snapshots")
      )
  }

  val thePlayVersion = play.core.PlayVersion.current
  val slf4jVersion = "1.7.10"
  val hystrixVersion = "1.3.20"
  val httpClientVersion = "4.4"
  val scalikejdbcVersion = "2.2.3"
  val swaggerVersion = "1.3.12"
  val jacksonVersion = "2.5.1"

  // Logging
  val logbackClassic      = "ch.qos.logback"            % "logback-classic"               % "1.1.2"
  val slf4jApi            = "org.slf4j"                 % "slf4j-api"                     % slf4jVersion
  val jclOverSlf4j        = "org.slf4j"                 % "jcl-over-slf4j"                % slf4jVersion
  val log4jOverSlf4j      = "org.slf4j"                 % "log4j-over-slf4j"              % slf4jVersion
  val julToSlf4j          = "org.slf4j"                 % "jul-to-slf4j"                  % slf4jVersion
  val ravenLogback        = "net.kencochrane.raven"     % "raven-logback"                 % "6.0.0"   % Runtime
  val janino              = "org.codehaus.janino"       % "janino"                        % "2.7.8"
  val ltsvLogger          = "com.beachape"              %% "ltsv-logger"                  % "0.0.8"

  // Hystrix
  val hystrixCore         = "com.netflix.hystrix"       % "hystrix-core"                  % hystrixVersion
  val hystrixStream       = "com.netflix.hystrix"       % "hystrix-metrics-event-stream"  % hystrixVersion
  val rxJavaScala         = "io.reactivex"              %% "rxscala"                      % "0.23.0" // matches the version rxjava used in hystrix-core

  // HTTP clients
  val asyncHttpClient     = "com.ning"                  % "async-http-client"             % "1.9.10"
  val httpClient          = "org.apache.httpcomponents" % "httpclient"                    % httpClientVersion
  val httpClientCache     = "org.apache.httpcomponents" % "httpclient-cache"              % httpClientVersion
  val metricsHttpClient   = "io.dropwizard.metrics"     % "metrics-httpclient"            % "3.1.0"

  // DB
  val postgres            = "org.postgresql"            % "postgresql"                    % "9.4-1200-jdbc41"   % Runtime
  val skinnyOrm           = "org.skinny-framework"      %% "skinny-orm"                   % "1.3.13"
  val scalikeJdbc         = "org.scalikejdbc"           %% "scalikejdbc"                  % scalikejdbcVersion
  val scalikeJdbcConfig   = "org.scalikejdbc"           %% "scalikejdbc-config"           % scalikejdbcVersion
  val scalikeJdbcPlay     = "org.scalikejdbc"           %% "scalikejdbc-play-plugin"      % "2.3.5"
  val dbcp2               = "org.apache.commons"        % "commons-dbcp2"                 % "2.0.1"

  // Memcached
  val shade               = "com.bionicspirit"          %% "shade"                        % "1.6.0"
  val spyMemcached        = "net.spy"                   % "spymemcached"                  % "2.11.6"

  // Play plugins
  val playFlyway          = "com.github.tototoshi"      %% "play-flyway"                  % "1.2.1"
  val scaldiPlay          = "org.scaldi"                %% "scaldi-play"                  % "0.5.3"
  val metricsPlay         = "com.kenshoo"               %% "metrics-play"                 % "2.3.0_0.1.8"
  val providedPlay        = "com.typesafe.play"         %% "play"                         % thePlayVersion      % Provided

  // Swagger
  val swaggerPlay         = "com.wordnik"               %% "swagger-play2"                % swaggerVersion
  val swaggerAnnotations  = "com.wordnik"               % "swagger-annotations"           % swaggerVersion

  // Jackson
  val jacksonCore         = "com.fasterxml.jackson.core"   % "jackson-core"               % jacksonVersion
  val jacksonScala        = "com.fasterxml.jackson.module" %% "jackson-module-scala"      % jacksonVersion
  val jacksonDatabind     = "com.fasterxml.jackson.core"   % "jackson-databind"           % jacksonVersion

  // Test
  val playTest            = "com.typesafe.play"         %% "play-test"                    % thePlayVersion      % Test
  val scalatest           = "org.scalatest"             %% "scalatest"                    % "2.2.4"             % Test
  val scalatestPlay       = "org.scalatestplus"         %% "play"                         % "1.2.0"             % Test
  val scalacheck          = "org.scalacheck"            %% "scalacheck"                   % "1.12.2"            % Test
  val groovy              = "org.codehaus.groovy"       % "groovy"                        % "2.4.0"             % Test
  val scalikeJdbcTest     = "org.scalikejdbc"           %% "scalikejdbc-test"             % scalikejdbcVersion  % Test

  // Misc utils
  val commonsValidator    = "commons-validator"         % "commons-validator"             % "1.4.1"             % Runtime
  val guava               = "com.google.guava"          % "guava"                         % "18.0"
  val jta                 = "javax.transaction"         % "jta"                           % "1.1"
  val scalaUri            = "com.netaporter"            %% "scala-uri"                    % "0.4.5"
  val findbugs            = "com.google.code.findbugs"  % "jsr305"                        % "3.0.0"

  val zipkinFutures       = "com.beachape"              %% "zipkin-futures-play"          % "0.0.7"

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
    commonsValidator,
    jta,
    scalaUri,

    // Play plugins
    playFlyway,
    scaldiPlay,
    metricsPlay,
    swaggerPlay,

    // Test
    playTest,
    scalatest,
    scalatestPlay,
    scalacheck,
    groovy,
    scalikeJdbcTest
  ).map(withoutExcluded)

  val playScalatestDependencies = Seq(
    scalatest,
    scalatestPlay
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

  // TODO when bumping to Play 2.4, depend on play-json instead of ws
  val playJsonFormatsDependencies = playScalatestDependencies :+ ws

  val scalaWsClientDependencies = playScalatestDependencies :+ ws
}