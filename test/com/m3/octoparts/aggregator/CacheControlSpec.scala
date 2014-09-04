package com.m3.octoparts.aggregator

import org.scalatest._
import org.joda.time.DateTimeUtils
import com.m3.octoparts.model.CacheControl
import com.m3.octoparts.cache.RichCacheControl._

class CacheControlSpec extends FunSpec with Matchers with BeforeAndAfter {

  before {
    DateTimeUtils.setCurrentMillisFixed(2222L)
  }

  it("is a good day to expire") {
    CacheControl(expiresAt = None).hasExpired shouldBe true

    CacheControl(expiresAt = Some(1111L)).hasExpired shouldBe true
    CacheControl(expiresAt = Some(3333L)).hasExpired shouldBe false

  }

  after {
    DateTimeUtils.setCurrentMillisSystem()
  }
}
