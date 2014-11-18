package com.m3.octoparts.util

import org.apache.commons.lang3.mutable.MutableBoolean
import org.scalatest.{ FunSpec, Matchers }

class KeyedResourcePoolSpec extends FunSpec with Matchers {

  it("should clean up obsolete clients") {

    object holder extends KeyedResourcePool[String, String, Int] {
      var i = 0
      var j = 0

      def makeKey(s: String) = s

      def makeNew(k: String) = {
        i += 1
        i
      }

      override def onRemove(value: Int): Unit = {
        j += value
      }
    }
    holder.getOrCreate("a") should be(1)

    // return existing
    holder.getOrCreate("a") should be(1)

    // new one
    holder.getOrCreate("b") should be(2)

    //clean up
    holder.cleanObsolete(Set("b"))
    holder.j should be(1)

    holder.getOrCreate("a") should be(3)

  }

  it("should never return closed items") {
    val firstTime = new MutableBoolean(false)
    object holder extends KeyedResourcePool[String, String, MutableBoolean] {
      def makeKey(s: String) = s

      def makeNew(k: String) = {
        if (firstTime.booleanValue()) {
          firstTime.setValue(false)
          // do a map.get within another map.get
          getOrCreate("a").booleanValue() should be(false)
        }

        new MutableBoolean(false)
      }

      def onRemove(value: MutableBoolean) = value.setValue(true) // true represents "closed"
    }

    holder.getOrCreate("a").booleanValue() should be(false)

    // failure cause bug #85949
    holder.getOrCreate("a").booleanValue() should be(false)
  }

}
