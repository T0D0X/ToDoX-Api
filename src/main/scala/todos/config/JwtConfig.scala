package todos.config

import zio.{ZIO, ZLayer}

import pureconfig.{ConfigReader, ConfigSource}

case class JwtConfig(
    secret: String,
    issuer: String,
    ttl: Long,
) derives ConfigReader

object JwtConfig {
  val live = ZLayer.fromZIO(ZIO.attempt(ConfigSource.default.at("jwt").loadOrThrow[JwtConfig]))
}
