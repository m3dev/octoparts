package com.m3.octoparts.aggregator.handler

import com.m3.octoparts.model.config.HttpPartConfig

trait HttpHandlerFactory {

  /**
   * @param config a HttpCommandConfig entry
   * @return a handler ready to be used
   */
  def makeHandler(config: HttpPartConfig): Handler

}
