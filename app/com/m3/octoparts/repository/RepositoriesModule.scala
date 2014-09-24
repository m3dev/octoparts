package com.m3.octoparts.repository

import com.m3.octoparts.cache.client.Cache
import com.m3.octoparts.http.HttpClientPool
import scaldi.Module

class RepositoriesModule extends Module {

  bind[MutableConfigsRepository] to
    new MutableCachingRepository(
      DBConfigsRepository,
      inject[Cache],
      inject[HttpClientPool]
    )(scala.concurrent.ExecutionContext.global)

}
