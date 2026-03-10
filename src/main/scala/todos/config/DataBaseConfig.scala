package todos.config
import com.typesafe.config.ConfigFactory
import doobie.Transactor
import zio.interop.catz.asyncInstance
import zio.{Task, ZIO, ZLayer}

import java.util.Properties

case class DataBaseConfig(
    user: String,
    password: String,
    host: String,
    name: String,
    port: Int
)

object DataBaseConfig {
  val layer: ZIO[Any, Throwable, Transactor[Task]] =
    for {
      config <- ZIO.attempt(ConfigFactory.load().getConfig("db"))

      dbConfig <- ZIO.attempt {
        DataBaseConfig(
          host = config.getString("host"),
          port = config.getInt("port"),
          name = config.getString("name"),
          user = config.getString("user"),
          password = config.getString("password")
        )
      }

      jdbcUrl = s"jdbc:postgresql://${dbConfig.host}:${dbConfig.port}/${dbConfig.name}"

      transactor <- ZIO.attempt {
        val props = new Properties()
        props.setProperty("user", dbConfig.user)
        props.setProperty("password", dbConfig.password)
        Transactor.fromDriverManager[Task](
          driver = "org.postgresql.Driver",
          url = jdbcUrl,
          props,
          logHandler = None
        )
      }
    } yield transactor

  val transactorLayer: ZLayer[Any, Throwable, Transactor[Task]] = ZLayer.fromZIO(DataBaseConfig.layer)
}
