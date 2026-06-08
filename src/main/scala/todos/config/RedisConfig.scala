package todos.config

import scala.concurrent.duration.Duration
import zio.ZLayer

import pureconfig.{ConfigReader, ConfigSource}

case class RedisConfig(
    uri: String,
    user: String,
    password: String,
    timeouts: Map[String, Duration],
) derives ConfigReader

object RedisConfig {
  val live = ZLayer.succeed(ConfigSource.default.at("redis").loadOrThrow[RedisConfig])
}
