package todos.config

import scala.concurrent.duration.Duration
import zio.ZLayer

import pureconfig.{ConfigReader, ConfigSource}

case class RedisConfig(
    uri: String,
    user: String,
    password: String,
    ttl: TtlConfig,
) derives ConfigReader

object RedisConfig {
  val live = ZLayer.succeed(ConfigSource.default.at("redis").loadOrThrow[RedisConfig])
}

case class TtlConfig(
    defaultTtl: Duration,
    customTtl: Map[String, Duration],
) derives ConfigReader {
  def ttlForNamespace(nameSpace: String): Duration = customTtl.getOrElse(nameSpace, defaultTtl)
}
