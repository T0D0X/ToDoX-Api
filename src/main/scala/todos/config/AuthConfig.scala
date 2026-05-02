package todos.config

import pureconfig.{ConfigReader, ConfigSource}
import zio.{ZIO, ZLayer}

case class AuthConfig(authToken: String) derives ConfigReader

object AuthConfig {
  val live = ZLayer.fromZIO(ZIO.attempt(ConfigSource.default.at("auth").loadOrThrow[AuthConfig]))
}
