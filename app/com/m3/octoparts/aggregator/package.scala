package com.m3.octoparts

import com.m3.octoparts.aggregator.handler.AggregatorHandlersModule
import com.m3.octoparts.aggregator.service.AggregatorServicesModule

package object aggregator {

  // Note: this has to be a def, not a val, to avoid "Injector already frozen" errors in tests
  def module = new AggregatorHandlersModule :: new AggregatorServicesModule

}
