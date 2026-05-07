package todos.config

import pureconfig.{ConfigReader, ConfigSource}
import zio.{ZIO, ZLayer}

case class JwtConfig(
    secret: String,
    issuer: String,
    ttl: Long,
) derives ConfigReader

object JwtConfig {
  val live = ZLayer.fromZIO(ZIO.attempt(ConfigSource.default.at("jwt").loadOrThrow[JwtConfig]))
}
