package com.m3.octoparts.util

import java.util.concurrent.{ ConcurrentHashMap, ConcurrentMap }

/**
 * A pool of key-value pairs.
 * It has a method to build a new value and a method to clean up all values that are no longer needed.
 */
trait KeyedResourcePool[K, V] {
  private val holder: ConcurrentMap[K, V] = new ConcurrentHashMap[K, V]()

  /**
   * Factory method to create a new element
   */
  protected def makeNew(key: K): V

  /**
   * Listener that is run after a value is removed
   * @param value the value that was removed
   */
  protected def onRemove(value: V): Unit

  /**
   * Get the value corresponding to the given key.
   * If no such value existed, a new one is created.
   */
  def getOrCreate(key: K): V = Option(holder.get(key)) match {
    case Some(v) => v
    case None =>
      val d = makeNew(key)
      val old = holder.put(key, d)
      // once d has been put, old (if it went in before last get) must be discarded properly
      if (old != null) {
        onRemove(old)
      }
      d
  }

  /**
   * Remove any elements whose keys are not contained in the given set.
   * [[onRemove]] will be called for each element after it is removed.
   *
   * @param validKeys all keys that you want to keep
   */
  final def cleanObsolete(validKeys: Set[K]): Unit = {
    val it = holder.entrySet().iterator
    while (it.hasNext) {
      val next = it.next()
      if (!validKeys.contains(next.getKey)) {
        // Must remove before evicting, or callers might get an evicted value
        it.remove()

        onRemove(next.getValue)
      }
    }
  }

  /**
   * Remove all elements from the pool, calling [[onRemove]] for each one in turn.
   */
  final def shutdown(): Unit = cleanObsolete(Set.empty)

}
