package todos.redis

import todos.config.{RedisConfig, TtlConfig}

import izumi.reflect.Tag
import zio.{Task, ZIO, ZLayer}

import io.circe.{Decoder, Encoder}
import io.circe.parser.decode
import io.circe.syntax.*
import io.lettuce.core.api.async.RedisAsyncCommands
import io.micrometer.core.instrument.{Counter, MeterRegistry, Timer}

trait RedisCache[K, V] {
  def get(key: K): Task[Option[V]]
  def set(key: K, value: V): Task[Unit]
  def del(key: K): Task[Unit]
}

class RedisCacheImpl[K: Encoder, V: Encoder: Decoder](
    redis: RedisAsyncCommands[String, String],
    nameSpace: String,
    registry: MeterRegistry,
    ttlConfig: TtlConfig,
) extends RedisCache[K, V] {
  private def encodeKey(key: K): String = s"$nameSpace:${key.asJson.noSpaces}"
  private val ttl = ttlConfig.ttlForNamespace(nameSpace).toSeconds

  private val requestsCounter = Counter
    .builder("redis_cache_requests_total")
    .description("Total number of Redis cache requests")
    .tag("namespace", nameSpace)

  private val latencyTimer = Timer
    .builder("redis_cache_latency")
    .tags("namespace", nameSpace)

  private def metered[A](operation: Task[A], command: String): Task[A] =
    operation.either.timed.flatMap { case (duration, exit) =>
      val (status, resultZIO) = exit match {
        case Right(value) => ("success", ZIO.succeed(value))
        case Left(error) => ("error", ZIO.fail(error))
      }
      ZIO.succeed {
        requestsCounter
          .tag("command", command)
          .tag("status", status)
          .register(registry)
          .increment()

        latencyTimer
          .tag("command", command)
          .tag("status", status)
          .register(registry)
          .record(duration.abs())
      } *> resultZIO
    }

  def get(key: K): Task[Option[V]] =
    metered(
      ZIO.fromCompletionStage(redis.get(encodeKey(key))).map(decode[V](_).toOption),
      "GET",
    )

  def set(key: K, value: V): Task[Unit] =
    metered(
      ZIO.fromCompletionStage(redis.setex(encodeKey(key), ttl, value.asJson.noSpaces)).unit,
      "SET",
    )

  def del(key: K): Task[Unit] =
    metered(
      ZIO.fromCompletionStage(redis.del(encodeKey(key))).unit,
      "DEL",
    )
}

object RedisCache {
  def live[K: Encoder: Tag, V: Encoder: Decoder: Tag](
      nameSpace: String,
  ): ZLayer[RedisAsyncCommands[String, String] & MeterRegistry & RedisConfig, Nothing, RedisCache[K, V]] =
    ZLayer {
      for {
        commands <- ZIO.service[RedisAsyncCommands[String, String]]
        registry <- ZIO.service[MeterRegistry]
        config <- ZIO.service[RedisConfig]
      } yield new RedisCacheImpl[K, V](commands, nameSpace, registry, config.ttl)
    }
}
