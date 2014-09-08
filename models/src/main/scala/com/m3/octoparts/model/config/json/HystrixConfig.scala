package com.m3.octoparts.model.config.json

case class HystrixConfig(timeoutInMs: Long,
                         threadPoolConfig: ThreadPoolConfig,
                         commandKey: String,
                         commandGroupKey: String)