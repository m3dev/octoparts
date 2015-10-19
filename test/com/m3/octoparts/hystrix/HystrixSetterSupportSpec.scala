package com.m3.octoparts.hystrix

import com.m3.octoparts.support.mocks.ConfigDataMocks
import org.scalatest._

class HystrixSetterSupportSpec extends FunSpec with Matchers with ConfigDataMocks {

  val subject = new HystrixSetterSupport {}

  describe("#threadPoolSetter") {

    it("should return a Hystrix ThreadPool setter with queue size and queue threshold size set to the config item's queue size") {
      val r = subject.threadPoolSetter(mockThreadConfig)
      r.getMaxQueueSize shouldBe mockThreadConfig.queueSize
      r.getQueueSizeRejectionThreshold shouldBe mockThreadConfig.queueSize
    }

  }

}
