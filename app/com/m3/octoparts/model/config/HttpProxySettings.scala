package com.m3.octoparts.model.config

import com.netaporter.uri.config.UriConfig
import com.netaporter.uri.parsing.DefaultUriParser
import org.parboiled2._

import scala.util.Try

case class HttpProxySettings(scheme: String, host: String, port: Int) {

  import com.m3.octoparts.model.config.HttpProxySettings._

  /**
   * inverse operation of [[HttpProxySettings.parse]]
   * @return a string representation, omitting fiels with default values
   */
  def serialize: String = {
    val schemePart = if (scheme == DefaultScheme) "" else s"$scheme://"
    val portPart = if (port == DefaultPort) "" else s":$port"
    s"$schemePart$host$portPart"
  }
}

object HttpProxySettings {
  private val DefaultScheme = "http"
  private val DefaultPort = -1

  /**
   * Defines proxy settings. Format is [scheme://]host[:port]
   *
   * Proxy credentials are not supported yet.
   */
  def parse(ser: String): Try[HttpProxySettings] = new HttpProxySettingsParser(ser).proxyRule.run()

  /**
   * @return whether the input could be parsed. See [[parse]]
   */
  def isValid(ser: String): Boolean = parse(ser).isSuccess

  private class HttpProxySettingsParser(input: ParserInput) extends DefaultUriParser(input, UriConfig.default) {
    import HttpProxySettingsParser._
    import org.parboiled2._

    def proxyRule: Rule1[HttpProxySettings] = rule {
      optional(_scheme ~ "://") ~ _host_name ~ optional(_port) ~ EOI ~> extractHttpProxySettings
    }

  }

  private object HttpProxySettingsParser {

    private val extractHttpProxySettings: (Option[String], String, Option[String]) => HttpProxySettings = {
      (optScheme: Option[String], host: String, portStr: Option[String]) =>
        val scheme = optScheme.getOrElse(DefaultScheme)
        val port = portStr.map(_.toInt).filter(p => p >= Char.MinValue && p <= Char.MaxValue).getOrElse(DefaultPort)
        HttpProxySettings(scheme, host, port)
    }
  }
}