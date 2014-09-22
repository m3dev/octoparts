package com.m3.octoparts.cache

import com.m3.octoparts.cache.RichCacheControl._
import com.m3.octoparts.model.CacheControl
import org.joda.time.DateTimeUtils
import org.scalatest._

class RichCacheControlSpec extends FunSpec with Matchers with BeforeAndAfter {

  before {
    DateTimeUtils.setCurrentMillisFixed(2222L)
  }

  it("is a good day to expire") {
    // No max-age -> we should treat it as expired
    CacheControl(expiresAt = None).hasExpired shouldBe true

    CacheControl(expiresAt = Some(1111L)).hasExpired shouldBe true
    CacheControl(expiresAt = Some(3333L)).hasExpired shouldBe false
  }

  after {
    DateTimeUtils.setCurrentMillisSystem()
  }
}
