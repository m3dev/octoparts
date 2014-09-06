package com.m3.octoparts.model.config.json

case class HystrixConfig(httpPartConfigId: Long,
                         timeoutInMs: Long,
                         threadPoolConfig: ThreadPoolConfig,
                         commandKey: String,
                         commandGroupKey: String)