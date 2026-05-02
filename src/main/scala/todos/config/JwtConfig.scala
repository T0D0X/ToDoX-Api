package todos.config

import pureconfig.{ConfigReader, ConfigSource}
import zio.ZIO

case class JwtConfig(
    secret: String,
    issuer: String,
    ttl: Long,
) derives ConfigReader

object JwtConfig {
  val jwtL: ZIO[Any, Throwable, JwtConfig] =
    ZIO.attempt(ConfigSource.default.at("jwt").loadOrThrow[JwtConfig])
}
