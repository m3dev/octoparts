package com.m3.octoparts.http

import scaldi.Module

class HttpModule extends Module {

  bind[HttpClientPool] to new HttpClientPool destroyWith (_.shutdown())

}
