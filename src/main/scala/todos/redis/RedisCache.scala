package todos.redis

import io.circe.parser.decode
import io.circe.syntax.*
import io.circe.{Decoder, Encoder}
import io.lettuce.core.api.async.RedisAsyncCommands
import io.micrometer.core.instrument.{Counter, MeterRegistry, Timer}
import izumi.reflect.Tag
import zio.{Duration, Task, ZIO, ZLayer}

class RedisCache[K: Encoder, V: Encoder: Decoder](
    redis: RedisAsyncCommands[String, String],
    nameSpace: String,
    registry: MeterRegistry,
) {
  private def encodeKey(key: K): String = s"$nameSpace:${key.asJson.noSpaces}"

  private def metered[A](operation: Task[A], command: String): Task[A] =
    operation.either.timed.flatMap { case (duration, exit) =>
      val (status, resultZIO) = exit match {
        case Right(value) => ("success", ZIO.succeed(value))
        case Left(error) => ("error", ZIO.fail(error))
      }
      ZIO.succeed {
        Counter
          .builder("redis_cache_requests_total")
          .tags("namespace", nameSpace, "command", command, "status", status)
          .register(registry)
          .increment()
        Timer
          .builder("redis_cache_latency")
          .tags("namespace", nameSpace, "command", command, "status", status)
          .register(registry)
          .record(duration)
      } *> resultZIO
    }

  def get(key: K): Task[Option[V]] =
    metered(
      ZIO.fromCompletionStage(redis.get(encodeKey(key))).map(decode[V](_).toOption),
      "GET",
    )

  def set(key: K, value: V, ttl: Duration): Task[Unit] =
    metered(
      ZIO.fromCompletionStage(redis.setex(encodeKey(key), ttl.toSeconds, value.asJson.noSpaces)).unit,
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
  ): ZLayer[RedisAsyncCommands[String, String] & MeterRegistry, Nothing, RedisCache[K, V]] =
    ZLayer {
      for {
        commands <- ZIO.service[RedisAsyncCommands[String, String]]
        registry <- ZIO.service[MeterRegistry]
      } yield new RedisCache[K, V](commands, nameSpace, registry)
    }
}
