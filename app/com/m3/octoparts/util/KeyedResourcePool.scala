package com.m3.octoparts.util

import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * A pool of key-value pairs.
 * It has a method to build a new value and a method to clean up all values that are no longer needed.
 */
trait KeyedResourcePool[K, V] {

  // Locks
  private val rwl = new ReentrantReadWriteLock()
  private val rlock = rwl.readLock
  private val wlock = rwl.writeLock

  private var holder: Map[K, V] = Map.empty[K, V]

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
  final def getOrCreate(key: K): V = {
    def get: Option[V] = {
      rlock.lock()
      try {
        holder get key
      } finally { rlock.unlock() }
    }
    def create: V = {
      wlock.lock()
      try {
        get getOrElse {
          val d = makeNew(key)
          holder = holder + (key -> d)
          d
        }
      } finally { wlock.unlock() }
    }
    get getOrElse create
  }

  /**
   * Remove any elements whose keys are not contained in the given set.
   * [[onRemove]] will be called for each element after it is removed.
   *
   * @param validKeys all keys that you want to keep
   */
  final def cleanObsolete(validKeys: K => Boolean): Unit = {
    wlock.lock()
    try {
      holder.foreach {
        case (key, value) =>
          if (!validKeys(key)) {
            holder = holder - key
            onRemove(value)
          }
      }
    } finally { wlock.unlock() }
  }

  /**
   * Remove all elements from the pool, calling [[onRemove]] for each one in turn.
   */
  final def shutdown(): Unit = cleanObsolete(Set.empty)

}
