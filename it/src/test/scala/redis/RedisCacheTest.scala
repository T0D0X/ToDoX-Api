package redis

import todos.config.RedisConfig
import todos.redis.{RedisCache, RedisConnection}

import scala.jdk.CollectionConverters.*
import zio.{Duration, Scope, ZIO, ZLayer}
import zio.test.*
import zio.test.TestAspect.*

import io.circe.syntax.*
import io.lettuce.core.api.async.RedisAsyncCommands
import io.micrometer.core.instrument.simple.SimpleMeterRegistry

object RedisCacheTest extends ZIOSpecDefault {

  val cacheLayer: ZLayer[RedisConfig & RedisAsyncCommands[String, String], Nothing, RedisCache[String, String]] =
    ZLayer.service[RedisAsyncCommands[String, String]] ++
      ZLayer.service[RedisConfig] ++
      ZLayer.succeed(new SimpleMeterRegistry()) >>>
      RedisCache.live[String, String]("test")

  val connectionLayer
      : ZLayer[Any, Throwable, RedisAsyncCommands[String, String] & RedisCache[String, String]] =
    (RedisConfig.live >>> RedisConnection.live) ++
      (RedisConfig.live ++ (RedisConfig.live >>> RedisConnection.live) >>> cacheLayer)

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
        _ <- repo.set(key1, value)
        result <- repo.get(key1)
      } yield assertTrue(result.contains(value))
    },
    test("1 return value 2 return empty") {
      for {
        repo <- ZIO.service[RedisCache[String, String]]
        _ <- repo.set(key1, value)
        results <- repo.get(key1) <&> repo.get(key2)
        (resultA, resultB) = results
      } yield assertTrue(resultA.contains(value), resultB.isEmpty)
    },
  )

  def setSpec = suite("set")(
    test("set value") {
      for {
        repo <- ZIO.service[RedisCache[String, String]]
        _ <- repo.set(key1, value)
        result <- repo.get(key1)
      } yield assertTrue(result.contains(value))
    },
    test("update value") {
      val value2 = "new value"
      for {
        repo <- ZIO.service[RedisCache[String, String]]
        _ <- repo.set(key1, value)
        _ <- repo.set(key1, value2)
        result <- repo.get(key1)
      } yield assertTrue(result.contains(value2))
    },
    test("timeout value") {
      for {
        repo <- ZIO.service[RedisCache[String, String]]
        _ <- repo.set(key1, value)
        _ <- ZIO.sleep(Duration.fromSeconds(1))
        result <- repo.get(key1)
      } yield assertTrue(result.isEmpty)
    } @@ TestAspect.withLiveClock,
  )
  def delSpec = suite("del")(
    test("success is value is non empty") {
      for {
        repo <- ZIO.service[RedisCache[String, String]]
        _ <- repo.set(key1, value)
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
