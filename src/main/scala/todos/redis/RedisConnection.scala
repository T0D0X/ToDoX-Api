package todos.redis

import io.lettuce.core.{RedisClient, RedisURI}
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.resource.ClientResources
import todos.config.RedisConfig
import zio.{Task, ZIO, ZLayer}

class RedisConnection private (
    client: RedisClient,
    connection: RedisAsyncCommands[String, String],
    resources: ClientResources,
) {
  def commands: RedisAsyncCommands[String, String] = connection

  private def shutdown(): Unit = {
    client.shutdown()
    resources.shutdown()
  }
}

object RedisConnection {
  def make(config: RedisConfig): Task[RedisConnection] =
    ZIO.attempt {
      val uri = RedisURI.create(config.uri)
      uri.setAuthentication(config.user, config.password)

      val resources = ClientResources.create()

      val client = RedisClient.create(resources, uri)
      val connection = client.connect().async()

      new RedisConnection(client, connection, resources)
    }

  val live: ZLayer[RedisConfig, Throwable, RedisAsyncCommands[String, String]] =
    ZLayer.scoped {
      for {
        config <- ZIO.service[RedisConfig]
        conn <- ZIO.acquireRelease(make(config))(c => ZIO.attempt(c.shutdown()).orDie)
      } yield conn.commands
    }
}
