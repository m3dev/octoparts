package com.m3.octoparts.wiring.assembling

import com.typesafe.config.ConfigFactory
import play.api.{ Environment, Logger, Configuration }

trait EnvConfigLoader {

  /**
   * Based on the current config, loads properties/config from an env-specific application.$env.conf file,
   * combining it with the passed-in config
   */
  protected def withEnvConfig(baseConfig: Configuration, environment: Environment): Configuration = {
    val mode = environment.mode
    val playEnv = baseConfig.getString("application.env").fold(mode.toString.toLowerCase) { parsedEnv =>
      /* "test" mode should cause the environment to be "test" except when
          the parsedEnv is "ci", since CI/Jenkins needs it's own test
          environment configuration
      */
      (mode.toString.toLowerCase, parsedEnv.toLowerCase) match {
        case ("test", env) if env != "ci" => "test"
        case (_, env) => env
      }
    }
    Logger.debug(s"Play environment $playEnv")
    val envSpecificConfigFileName = s"application.$playEnv.conf"
    if (configFileResolvable(envSpecificConfigFileName, environment)) {
      baseConfig ++ Configuration(ConfigFactory.load(envSpecificConfigFileName))
    } else {
      Logger.info(s"Could not load env-specific conf for: $playEnv perhaps $envSpecificConfigFileName does not exist")
      baseConfig
    }
  }

  protected def configFileResolvable(configFileName: String, env: Environment): Boolean = {
    Option(env.classLoader.getResource(configFileName)).isDefined
  }

}