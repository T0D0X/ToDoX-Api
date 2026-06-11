package todos.config

import zio.{ZIO, ZLayer}

import pureconfig.{ConfigReader, ConfigSource}

case class AuthConfig(authToken: String) derives ConfigReader

object AuthConfig {
  val live = ZLayer.fromZIO(ZIO.attempt(ConfigSource.default.at("auth").loadOrThrow[AuthConfig]))
}
