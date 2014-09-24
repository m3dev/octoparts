package com.m3.octoparts.cache.memcached

import org.scalatest.{ FunSpec, BeforeAndAfter, Matchers }
import org.joda.time.DateTimeUtils
import scala.concurrent.duration._
import com.m3.octoparts.model.CacheControl

class TtlCalculatorSpec extends FunSpec with Matchers with BeforeAndAfter {

  val now = 1401148800000L

  before {
    DateTimeUtils.setCurrentMillisFixed(now)
  }

  after {
    DateTimeUtils.setCurrentMillisSystem()
  }

  val ttlCalculator = new TtlCalculator {}

  describe("TtlCalculater") {

    describe("When both expiry and TTL are undefined") {
      it("should return None") {
        ttlCalculator.calculateTtl(CacheControl(expiresAt = None), None) should be(None)
      }
    }

    describe("When only expiresAt is defined") {
      it("should return a TTL calculated from expiresAt") {
        ttlCalculator.calculateTtl(CacheControl(expiresAt = Some(now + 10000L)), None) should be(Some(10.seconds))
      }
    }

    describe("When only configured TTL is defined") {
      it("should return the configured TTL") {
        ttlCalculator.calculateTtl(CacheControl(expiresAt = Some(now + 10000L)), None) should be(Some(10.seconds))
      }
    }

    describe("When expiresAt is after the configured TTL") {
      it("should return the configured TTL") {
        ttlCalculator.calculateTtl(CacheControl(expiresAt = Some(now + 10000L)), Some(3.seconds)) should be(Some(3.seconds))
      }
    }

    describe("When expiresAt is before the configured TTL") {
      it("should return a TTL calculated from expiresAt") {
        ttlCalculator.calculateTtl(CacheControl(expiresAt = Some(now + 5000L)), Some(10.seconds)) should be(Some(5.seconds))
      }
    }

    describe("When expiresAt is in the past") {
      it("should return a TTL of 0") {
        ttlCalculator.calculateTtl(CacheControl(expiresAt = Some(now - 1L)), Some(10.seconds)) should be(Some(Duration.Zero))
      }
    }

    describe("When expiresAt is before the configured TTL but there is a revalidation method") {
      it("should return the configured TTL") {
        ttlCalculator.calculateTtl(CacheControl(expiresAt = Some(now + 5000L), etag = Some("cafebabe")), Some(10.seconds)) should be(Some(10.seconds))
      }
    }
  }
}
