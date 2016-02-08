package com.m3.octoparts.support

import com.codahale.metrics.{ SharedMetricRegistries, MetricRegistry }
import com.kenshoo.play.metrics.Metrics
import com.m3.octoparts.support.db.{ RequiresDB, DBSuite }
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.openqa.selenium.safari.SafariDriver
import org.scalatest._
import org.scalatest.concurrent.{ ScalaFutures, IntegrationPatience, Eventually }
import org.scalatest.selenium.WebBrowser
import org.scalatestplus.play.{ PortNumber, BrowserFactory }
import org.scalatestplus.play.BrowserFactory.UnavailableDriver
import play.api._
import play.api.test._
import org.openqa.selenium.WebDriver

import com.m3.octoparts.wiring.OctopartsApplicationLoader

/*
 * This file holds a number of pre-made "test-harness" Specs for our own convenience
 */

trait MetricsSupport {

  implicit lazy val metrics = new Metrics {
    val defaultRegistry: MetricRegistry = SharedMetricRegistries.getOrCreate("sup-yo")
    val toJson: String = "{ yes: false }"
  }

}

/**
 * Base trait for any kind of spec that needs an Application or any of its components
 */
trait TestAppComponents {

  // Override as necessary
  lazy val context: ApplicationLoader.Context = {
    ApplicationLoader.createContext(
      Environment.simple()
    )
  }

  lazy val appComponents = {
    val applicationLoader: OctopartsApplicationLoader = new OctopartsApplicationLoader
    applicationLoader.components(context)
  }

  implicit lazy val app = appComponents.application

  lazy val messagesApi = appComponents.messagesApi

  lazy val actorSystem = appComponents.actorSystem

}

/**
 * FunSpec w/ matchers, pre-loaded with a bunch of Future-related goodies
 */
trait FutureFunSpec extends FunSpec with Matchers with ScalaFutures with IntegrationPatience

/**
 * Allows you to have one running compile-time DI app through-out a suite.
 *
 * DI components for the app can be accessed via the applicationBuilder member
 */
trait PlayAppSupport extends SuiteMixin with TestAppComponents with RequiresDB { this: Suite =>

  abstract override def run(testName: Option[String], args: Args): Status = {
    Play.start(app)
    try {
      val newConfigMap = args.configMap + ("org.scalatestplus.play.app" -> app)
      val newArgs = args.copy(configMap = newConfigMap)
      val status = super.run(testName, newArgs)
      status.whenCompleted { _ => Play.stop(app) }
      status
    } catch { // In case the suite aborts, ensure the app is stopped
      case ex: Throwable =>
        Play.stop(app)
        throw ex
    }
  }

}

/**
 * Be careful not to extend PlayAppSupport here or in any spec that uses this trait, because the run method will invoke
 * super.run.
 *
 * This is a problem because TestServer.start() calls Play.start, passing its Play application, and
 * so does PlayAppSupport's run method. When Play.start(app) is called twice with the same app,
 * <em>shutdown is called on the same app</em>, meaning various components will get shut down, leading
 * to funny things like failure to enqueue events on the Akka scheduler timer due to the timer having
 * been shut down before it has ever been used.
 */
trait PlayServerSupport extends SuiteMixin with TestAppComponents with RequiresDB { this: Suite =>

  /**
   * Implicit `PortNumber` instance that wraps `port`. The value returned from `portNumber.value`
   * will be same as the value of `port`.
   *
   * @return the configured port number, wrapped in a `PortNumber`
   */
  implicit final lazy val portNumber: PortNumber = PortNumber(port)

  /**
   * The port used by the `TestServer`.  By default this will be set to the result returned from
   * `Helpers.testServerPort`. You can override this to provide a different port number.
   */
  lazy val port: Int = Helpers.testServerPort

  /**
   * Invokes `start` on a new `TestServer` created with the `FakeApplication` provided by `app` and the
   * port number defined by `port`, places the `FakeApplication` and port number into the `ConfigMap` under the keys
   * `org.scalatestplus.play.app` and `org.scalatestplus.play.port`, respectively, to make
   *
   * them available to nested suites; calls `super.run`; and lastly ensures the `FakeApplication and test server are stopped after
   * all tests and nested suites have completed.
   *
   * @param testName an optional name of one test to run. If `None`, all relevant tests should be run.
   *                 I.e., `None` acts like a wildcard that means run all relevant tests in this `Suite`.
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when all tests and nested suites started by this method have completed, and whether or not a failure occurred.
   */
  abstract override def run(testName: Option[String], args: Args): Status = {
    val testServer = TestServer(port, app)
    testServer.start()
    try {
      val newConfigMap = args.configMap + ("org.scalatestplus.play.app" -> app) + ("org.scalatestplus.play.port" -> port)
      val newArgs = args.copy(configMap = newConfigMap)
      val status = super.run(testName, newArgs)
      status.whenCompleted { _ => testServer.stop() }
      status
    } catch { // In case the suite aborts, ensure the server is stopped
      case ex: Throwable =>
        testServer.stop()
        throw ex
    }
  }
}

trait OneDIBrowserPerSuite
    extends SuiteMixin
    with WebBrowser
    with Eventually
    with IntegrationPatience
    with BrowserFactory { this: Suite =>

  /**
   * Override this to your needs
   */
  val enableJavascript: Boolean = false

  /**
   * An implicit instance of `WebDriver`, created by calling `createWebDriver`.
   * If there is an error when creating the `WebDriver`, `UnavailableDriver` will be assigned
   * instead.
   */
  implicit lazy val webDriver: WebDriver = {
    val driver = createWebDriver()
    driver match {
      case d: HtmlUnitDriver => {
        d.setJavascriptEnabled(enableJavascript)
        d
      }
      case other => other
    }
  }

  /**
   * Automatically cancels tests with an appropriate error message when the `webDriver` field is a `UnavailableDriver`,
   * else calls `super.withFixture(test)`
   */
  abstract override def withFixture(test: NoArgTest): Outcome = {
    webDriver match {
      case UnavailableDriver(ex, errorMessage) =>
        ex match {
          case Some(e) => cancel(errorMessage, e)
          case None => cancel(errorMessage)
        }
      case _ => super.withFixture(test)
    }
  }

  /**
   * Places the `WebDriver` provided by `webDriver` into the `ConfigMap` under the key
   * `org.scalatestplus.play.webDriver` to make
   * it available to nested suites; calls `super.run`; and lastly ensures the `WebDriver` is stopped after
   * all tests and nested suites have completed.
   *
   * @param testName an optional name of one test to run. If `None`, all relevant tests should be run.
   *                 I.e., `None` acts like a wildcard that means run all relevant tests in this `Suite`.
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when all tests and nested suites started by this method have completed, and whether or not a failure occurred.
   */
  abstract override def run(testName: Option[String], args: Args): Status = {
    val cleanup: Boolean => Unit = { _ =>
      webDriver match {
        case _: UnavailableDriver => // do nothing for UnavailableDriver
        case safariDriver: SafariDriver => safariDriver.quit()
        case chromeDriver: ChromeDriver => chromeDriver.quit()
        case _ => webDriver.close()
      }
    }
    try {
      val newConfigMap = args.configMap + ("org.scalatestplus.play.webDriver" -> webDriver)
      val newArgs = args.copy(configMap = newConfigMap)
      val status = super.run(testName, newArgs)
      status.whenCompleted(cleanup)
      status
    } catch {
      case ex: Throwable =>
        cleanup(false)
        throw ex
    }
  }
}