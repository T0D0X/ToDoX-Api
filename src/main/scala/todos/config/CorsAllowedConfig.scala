package todos.config

import zio.{ZIO, ZLayer}
import zio.http.Method

import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.error.UserValidationFailed

case class CorsAllowedConfig(
    origins: Set[String],
    methods: Set[Method],
    headers: Set[String],
    credentials: Boolean,
) derives ConfigReader

object CorsAllowedConfig {
  given ConfigReader[Method] = ConfigReader[String].map(Method.fromString).emap {
    case Method.CUSTOM(str) => Left(UserValidationFailed(s"Custom method not allowed: $str"))
    case method => Right(method)
  }

  val live = ZLayer.fromZIO(ZIO.attempt(ConfigSource.default.at("cors").loadOrThrow[CorsAllowedConfig]))
}
