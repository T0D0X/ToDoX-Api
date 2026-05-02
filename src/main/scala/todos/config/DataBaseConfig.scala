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
    port: Int,
) derives ConfigReader

object DataBaseConfig {

  private def mkTransactor(cfg: DataBaseConfig): Transactor[Task] = {
    val url = s"jdbc:postgresql://${cfg.host}:${cfg.port}/${cfg.name}"
    val props = new Properties()
    props.setProperty("user", cfg.user)
    props.setProperty("password", cfg.password)
    Transactor.fromDriverManager[Task](
      driver = "org.postgresql.Driver",
      url = url,
      props,
      logHandler = None,
    )
  }

  val lZio: ZIO[Any, Throwable, Transactor[Task]] =
    ZIO.attempt(ConfigSource.default.at(s"db").loadOrThrow[DataBaseConfig]).map(mkTransactor)

  val live: ZLayer[Any, Throwable, Transactor[Task]] = ZLayer.fromZIO(lZio)
}
