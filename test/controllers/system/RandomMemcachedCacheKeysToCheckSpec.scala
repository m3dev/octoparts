package controllers.system

import org.scalatest.{ FunSpec, Matchers }

class RandomMemcachedCacheKeysToCheckSpec extends FunSpec with Matchers {

  it("should produce a sequence of size n") {
    val k = 13
    RandomMemcachedCacheKeysToCheck(k)() should have size k
  }
}
