package todos.config

import pureconfig.{ConfigReader, ConfigSource}
import zio.ZLayer
import scala.concurrent.duration.Duration

case class RedisConfig(
    uri: String,
    user: String,
    password: String,
    timeouts: Map[String, Duration],
) derives ConfigReader

object RedisConfig {
  val live = ZLayer.succeed(ConfigSource.default.at("redis").loadOrThrow[RedisConfig])
}
