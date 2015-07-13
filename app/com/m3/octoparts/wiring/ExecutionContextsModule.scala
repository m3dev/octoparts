package com.m3.octoparts.wiring

import akka.actor.ActorSystem
import play.api.Play
import play.api.libs.concurrent.Akka

trait ExecutionContextsModule extends UtilsModule {

  // To be filled in with the Play default actor system
  def actorSystem: ActorSystem

  lazy val dbFetchExecutionContext = actorSystem.dispatchers.lookup("contexts.db")
  lazy val partsServiceContext = actorSystem.dispatchers.lookup("contexts.parts-service")

}
