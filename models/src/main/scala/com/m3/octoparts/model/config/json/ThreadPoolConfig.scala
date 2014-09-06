package com.m3.octoparts.model.config.json

case class ThreadPoolConfig(threadPoolKey: String,
                            coreSize: Int,
                            queueSize: Int)
