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
  val slf4jVersion = "1.7.25"
  val hystrixVersion = "1.5.12"
  val httpClientVersion = "4.5.2"
  val scalikejdbcVersion = "2.4.3"
  val jacksonVersion = "2.5.5"
  val jacksonScalaVersion = "2.5.2"
  val macwireVersion = "2.2.5"

  // Logging
  val logbackClassic      = "ch.qos.logback"            % "logback-classic"               % "1.1.11"
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
  val hsytrixSerialization= "com.netflix.hystrix"       % "hystrix-serialization"         % hystrixVersion   % Runtime

  val rxJavaScala         = "io.reactivex"              %% "rxscala"                      % "0.26.5" // compatible with the rxjava (1.1.0) used in hystrix-core. Check again if you change.
  val rxReactiveStreams   = "io.reactivex"              % "rxjava-reactive-streams"       % "1.2.1"

  // HTTP clients
  val asyncHttpClient     = "com.ning"                  % "async-http-client"             % "1.9.21" // not upgrading because play-ws uses this version
  val httpClient          = "org.apache.httpcomponents" % "httpclient"                    % httpClientVersion
  val httpClientCache     = "org.apache.httpcomponents" % "httpclient-cache"              % httpClientVersion
  val metricsHttpClient   = "io.dropwizard.metrics"     % "metrics-httpclient"            % "3.1.4"

  // DB
  val postgres            = "org.postgresql"            % "postgresql"                    % "9.4.1211"          % Runtime
  val skinnyOrm           = "org.skinny-framework"      %% "skinny-orm"                   % "2.2.0"             excludeAll(
    // excluding to avoid upgrading flyway version
    // skinny's db migration module is not used at all in this project
    ExclusionRule(organization = "org.flywaydb", name = "flyway-core")
  )
  val scalikeJdbc         = "org.scalikejdbc"           %% "scalikejdbc"                  % scalikejdbcVersion
  val scalikeJdbcConfig   = "org.scalikejdbc"           %% "scalikejdbc-config"           % scalikejdbcVersion
  val scalikeJdbcPlay     = "org.scalikejdbc"           %% "scalikejdbc-play-initializer" % "2.4.5"
  val dbcp2               = "org.apache.commons"        % "commons-dbcp2"                 % "2.1.1"

  val flyway              = "org.flywaydb"              % "flyway-core"                   % "4.2.0"

  // Memcached
  val shade               = "com.bionicspirit"          %% "shade"                        % "1.7.4"

  // Play plugins
  val playFlyway          = "org.flywaydb"              %% "flyway-play"                  % "4.0.0"
  val providedPlay        = "com.typesafe.play"         %% "play"                         % thePlayVersion      % Provided

  // DI
  val macwireMacros       = "com.softwaremill.macwire"  %% "macros"                       % macwireVersion

  // Swagger
  val swaggerPlay26       = "io.swagger"                 %% "swagger-play2"                % "1.6.0-SNAPSHOT" // replace with offical when ready
  val swaggerAnnotations  = "io.swagger"                 % "swagger-annotations"           % "1.5.15"

  // Jackson
  val jacksonCore         = "com.fasterxml.jackson.core"   % "jackson-core"               % jacksonVersion
  val jacksonScala        = "com.fasterxml.jackson.module" %% "jackson-module-scala"      % jacksonScalaVersion
  val jacksonDatabind     = "com.fasterxml.jackson.core"   % "jackson-databind"           % jacksonVersion

  // Test
  val playTest            = "com.typesafe.play"         %% "play-test"                    % thePlayVersion      % Test

  val scalatest           = "org.scalatest"             %% "scalatest"                    % "3.0.3"             % Test
  val scalatestPlay       = "org.scalatestplus.play"    %% "scalatestplus-play"           % "3.0.0"             % Test
  val scalacheck          = "org.scalacheck"            %% "scalacheck"                   % "1.13.5"            % Test
  val groovy              = "org.codehaus.groovy"       %  "groovy"                       % "2.4.7"             % Test
  val scalikeJdbcTest     = "org.scalikejdbc"           %% "scalikejdbc-test"             % scalikejdbcVersion  % Test
  val mockitoCore         = "org.mockito"               % "mockito-core"                  % "1.10.19"           % Test

  // Misc utils
  val commonsValidator    = "commons-validator"         % "commons-validator"             % "1.4.1"             % Runtime
  val guava               = "com.google.guava"          % "guava"                         % "21.0"
  val jta                 = "javax.transaction"         % "jta"                           % "1.1"
  val scalaUri            = "com.netaporter"            %% "scala-uri"                    % "0.4.16"
  val findbugs            = "com.google.code.findbugs"  % "jsr305"                        % "3.0.2"

  val kenshoo             = "com.kenshoo"               %% "metrics-play"                 % "2.4.0_0.4.1"

  val zipkinFutures       = "com.beachape"              %% "zipkin-futures-play"          % "0.3.0"

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
    hsytrixSerialization,
    rxJavaScala,
    rxReactiveStreams,

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

    // Swagger
    swaggerPlay26,

    openId,

    // Test
    playTest,
    scalacheck,
    groovy,
    scalikeJdbcTest,
    ws
  ).map(withoutExcluded)

  val playScalatestDependencies = Seq(
    scalatest,
    scalatestPlay,
    mockitoCore
  )

  val authHandlerDependencies = Seq(
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

  val playJsonFormatsDependencies = playScalatestDependencies :+ "com.typesafe.play" %% "play-json" % "2.6.0-M7"

  val scalaWsClientDependencies = playScalatestDependencies :+ ws
}
