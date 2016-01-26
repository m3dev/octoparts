package com.m3.octoparts.util

import com.codahale.metrics.MetricRegistry
import com.m3.octoparts.support.PlayAppSupport
import org.scalatest.{ BeforeAndAfterEach, Matchers, FunSpec }
import play.api.Configuration
import scala.collection.JavaConverters._

class OctoMetricsImplSpec extends FunSpec with Matchers with PlayAppSupport with BeforeAndAfterEach {

  lazy val subject = {
    // Force metrics.jvm to be true
    val config = appComponents.configuration ++ Configuration.from(Map("metrics.jvm" -> true))
    new OctoMetricsImpl(appComponents.applicationLifecycle, config)
  }

  override protected def afterEach(): Unit = {
    subject.onStop()
  }

  private def ensureJvmMetrics(metricRegistry: MetricRegistry): Unit = {
    val metricNames = metricRegistry.getNames.asScala
    val expectedMetricNames = Seq("jvm.attribute", "jvm.gc", "jvm.memory", "jvm.threads")
    expectedMetricNames.foreach { metricName =>
      metricNames.find(_.contains(metricName)) shouldBe 'defined
    }
  }

  describe("#onStart") {

    it("should not throw") {
      subject.onStart()
    }

    it("should not throw with consecutive calls") {
      subject.onStart()
      subject.onStart()
    }

    it("should work if metrics were already set up") {
      val registry = subject.defaultRegistry
      subject.setupJvmMetrics(registry)
      ensureJvmMetrics(registry)
      subject.onStart()
      ensureJvmMetrics(registry)
    }

  }

}