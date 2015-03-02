package com.m3.octoparts.repository.config

import skinny.AbstractParamType

import scala.concurrent.duration.FiniteDuration

object ExtraParamType {

  case object DurationParamType extends AbstractParamType({
    case d: FiniteDuration => d.toSeconds
  })

  case object FineDurationParamType extends AbstractParamType({
    case d: FiniteDuration => d.toMillis
  })

  case object IntSetParamType extends AbstractParamType({
    // this will actually be a Set[Int] but the compiler complains about it.
    case s: Set[_] => s.mkString(",")
  })

}
