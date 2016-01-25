package com.m3.octoparts.util

import com.codahale.metrics.JvmAttributeGaugeSet
import com.codahale.metrics.jvm.{ GarbageCollectorMetricSet, MemoryUsageGaugeSet, ThreadStatesGaugeSet }
import com.m3.octoparts.support.PlayAppSupport
import org.scalatest.{ BeforeAndAfterEach, Matchers, FunSpec }
import scala.collection.JavaConverters._

class OctoMetricsImplSpec extends FunSpec with Matchers with PlayAppSupport with BeforeAndAfterEach {

  override protected def afterEach(): Unit = {
    subject.onStop()
  }

  lazy val subject = new OctoMetricsImpl(appComponents.applicationLifecycle, appComponents.configuration)

  describe("#onStart") {

    it("should not throw") {
      subject.onStart()
    }

    it("should work with consecutive calls") {
      subject.onStart()
      subject.onStart()
    }

    it("should work if metrics were already registered") {
      val registry = subject.defaultRegistry
      registry.register("jvm.attribute", new JvmAttributeGaugeSet())
      registry.register("jvm.gc", new GarbageCollectorMetricSet())
      registry.register("jvm.memory", new MemoryUsageGaugeSet())
      registry.register("jvm.threads", new ThreadStatesGaugeSet())
      subject.onStart()
      val metricNames = registry.getNames.asScala
      metricNames.find(_.contains("jvm.attribute")) shouldBe 'defined
      metricNames.find(_.contains("jvm.gc")) shouldBe 'defined
      metricNames.find(_.contains("jvm.memory")) shouldBe 'defined
      metricNames.find(_.contains("jvm.threads")) shouldBe 'defined
    }

  }

}