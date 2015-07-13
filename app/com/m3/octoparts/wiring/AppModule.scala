package com.m3.octoparts.wiring

import play.api.Application

trait AppModule {

  implicit def app: Application

}