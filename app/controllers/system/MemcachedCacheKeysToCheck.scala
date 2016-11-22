package controllers.system

trait MemcachedCacheKeysToCheck extends (() => Seq[String])

/**
 * Generates n random keys. Useful when your memcached setup has several non-redundant instances
 * @param n
 */
case class RandomMemcachedCacheKeysToCheck(
    n: Int
) extends MemcachedCacheKeysToCheck {
  import org.apache.commons.lang3.RandomStringUtils

  def apply() = (1 to n).map { _ => RandomStringUtils.randomAlphanumeric(10) }
}

object SingleMemcachedCacheKeyToCheck
    extends MemcachedCacheKeysToCheck {
  def apply() = Seq("ping")
}
