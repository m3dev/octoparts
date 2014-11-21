package com.m3.octoparts.util

/**
 * A pool of key-value pairs.
 * It has a method to build a new value and a method to clean up all values that are no longer needed.
 */
trait SyncedKeyedResourcePool[V] {
  private var holder: Map[Symbol, V] = Map.empty[Symbol, V]

  /**
   * Factory method to create a new element
   */
  protected def makeNew(key: Symbol): V

  /**
   * Listener that is run after a value is removed
   * @param value the value that was removed
   */
  protected def onRemove(value: V): Unit

  /**
   * Get the value corresponding to the given key.
   * If no such value existed, a new one is created.
   */
  final def getOrCreate(key: Symbol): V = key.synchronized {
    holder.get(key) match {
      case Some(v) => v
      case None => {
        val d = makeNew(key)
        holder = holder + (key -> d)
        d
      }
    }
  }

  /**
   * Remove any elements whose keys are not contained in the given set.
   * [[onRemove]] will be called for each element after it is removed.
   *
   * @param validKeys all keys that you want to keep
   */
  final def cleanObsolete(validKeys: Set[Symbol]): Unit = synchronized {
    holder.foreach {
      case (key, value) =>
        if (!validKeys.contains(key)) {
          holder = holder - key
          onRemove(value)
        }
    }
  }

  /**
   * Remove all elements from the pool, calling [[onRemove]] for each one in turn.
   */
  final def shutdown(): Unit = cleanObsolete(Set.empty)

}
