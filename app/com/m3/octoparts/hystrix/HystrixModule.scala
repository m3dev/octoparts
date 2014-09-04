package com.m3.octoparts.hystrix

import scaldi.Module

class HystrixModule extends Module {

  bind[HystrixHealthReporter] to HystrixHealthReporter

}
