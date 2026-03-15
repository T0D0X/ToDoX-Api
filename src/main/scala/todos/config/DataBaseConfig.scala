package todos.config
import doobie.Transactor
import pureconfig.*
import zio.interop.catz.asyncInstance
import zio.{Task, ZIO, ZLayer}

import java.util.Properties

case class DataBaseConfig(
    user: String,
    password: String,
    host: String,
    name: String,
    port: Int
) derives ConfigReader

object DataBaseConfig {

  private def mkTransactor(cfg: DataBaseConfig): Transactor[Task] = {
    val url   = s"jdbc:postgresql://${cfg.host}:${cfg.port}/${cfg.name}"
    val props = new Properties()
    props.setProperty("user", cfg.user)
    props.setProperty("password", cfg.password)
    Transactor.fromDriverManager[Task](
      driver = "org.postgresql.Driver",
      url = url,
      props,
      logHandler = None
    )
  }

  def loadMkTransactor(name: String): ZIO[Any, Throwable, Transactor[Task]] =
    ZIO.attempt(ConfigSource.default.at(s"db.$name").loadOrThrow[DataBaseConfig]).map(mkTransactor)
  
  val usersL: ZIO[Any, Throwable, Transactor[Task]] = loadMkTransactor("users")
  val todoL: ZIO[Any, Throwable, Transactor[Task]] = loadMkTransactor("todo_items")
  val usersLayer: ZLayer[Any, Throwable, Transactor[Task]] = ZLayer.fromZIO(usersL)
  val todoLayer: ZLayer[Any, Throwable, Transactor[Task]]  = ZLayer.fromZIO(todoL)
}
