package redis

import io.lettuce.core.api.async.RedisAsyncCommands
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import todos.config.RedisConfig
import io.circe.syntax.*
import todos.redis.{RedisCache, RedisConnection}
import zio.{Duration, Scope, ZIO, ZLayer}
import zio.test.*
import zio.test.TestAspect.*
import scala.jdk.CollectionConverters.*

object RedisCacheTest extends ZIOSpecDefault {
  private val commandsLayer: ZLayer[Any, Throwable, RedisAsyncCommands[String, String]] =
    RedisConfig.live >>> RedisConnection.live

  private val cacheLayer: ZLayer[RedisAsyncCommands[String, String], Nothing, RedisCache[String, String]] =
    ZLayer.service[RedisAsyncCommands[String, String]] ++
      ZLayer.succeed(new SimpleMeterRegistry()) >>>
      RedisCache.live[String, String]("test")

  private val connectionLayer: ZLayer[Any, Throwable, RedisCache[String, String] & RedisAsyncCommands[String, String]] =
    commandsLayer ++ (commandsLayer >>> cacheLayer)

  private val beforeEffect =
    ZIO.serviceWithZIO[RedisAsyncCommands[String, String]] { commands =>
      ZIO
        .fromCompletionStage(commands.keys("test:*"))
        .flatMap { keysRaw =>
          val keys = keysRaw.asScala.map(_.asJson.noSpaces).asJson.noSpaces
          ZIO.whenDiscard(keys.nonEmpty)(
            ZIO.fromCompletionStage(commands.del(keys)),
          )
        }
    }

  override def spec: Spec[TestEnvironment & Scope, Any] = {
    val testSuite = suite("RedisCacheTest")(
      getSpec,
      setSpec,
      delSpec,
    ) @@ sequential @@ TestAspect.before(beforeEffect)
    testSuite.provideLayer(connectionLayer)
  }

  private val (key1, key2) = ("key1", "key2")
  private val value = "value"
  private val deafultTime = Duration.fromSeconds(10)

  def getSpec = suite("get")(
    test("return Empty") {
      for {
        repo <- ZIO.service[RedisCache[String, String]]
        result <- repo.get(key1)
      } yield assertTrue(result.isEmpty)
    },
    test("return value") {
      for {
        repo <- ZIO.service[RedisCache[String, String]]
        _ <- repo.set(key1, value, deafultTime)
        result <- repo.get(key1)
      } yield assertTrue(result.contains(value))
    },
    test("1 return value 2 return empty") {
      for {
        repo <- ZIO.service[RedisCache[String, String]]
        _ <- repo.set(key1, value, deafultTime)
        results <- repo.get(key1) <&> repo.get(key2)
        (resultA, resultB) = results
      } yield assertTrue(resultA.contains(value), resultB.isEmpty)
    },
  )

  def setSpec = suite("set")(
    test("set value") {
      for {
        repo <- ZIO.service[RedisCache[String, String]]
        _ <- repo.set(key1, value, deafultTime)
        result <- repo.get(key1)
      } yield assertTrue(result.contains(value))
    },
    test("update value") {
      val value2 = "new value"
      for {
        repo <- ZIO.service[RedisCache[String, String]]
        _ <- repo.set(key1, value, deafultTime)
        _ <- repo.set(key1, value2, deafultTime)
        result <- repo.get(key1)
      } yield assertTrue(result.contains(value2))
    },
    test("timeout value") {
      for {
        repo <- ZIO.service[RedisCache[String, String]]
        _ <- repo.set(key1, value, Duration.fromSeconds(1))
        _ <- ZIO.sleep(Duration.fromSeconds(1))
        result <- repo.get(key1)
      } yield assertTrue(result.isEmpty)
    } @@ TestAspect.withLiveClock,
  )
  def delSpec = suite("del")(
    test("success is value is non empty") {
      for {
        repo <- ZIO.service[RedisCache[String, String]]
        _ <- repo.set(key1, value, deafultTime)
        resultBefore <- repo.get(key1)
        _ <- repo.del(key1)
        resultAfter <- repo.get(key1)
      } yield assertTrue(resultBefore.contains(value), resultAfter.isEmpty)
    },
    test("success is value is empty") {
      for {
        repo <- ZIO.service[RedisCache[String, String]]
        resultBefore <- repo.get(key1)
        _ <- repo.del(key1)
        resultAfter <- repo.get(key1)
      } yield assertTrue(resultBefore.isEmpty, resultAfter.isEmpty)
    },
  )
}
