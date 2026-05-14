package todos.config
import doobie.{ExecutionContexts, Transactor}
import org.postgresql.ds.PGSimpleDataSource
import pureconfig.*
import zio.interop.catz.asyncInstance
import zio.{Task, ZIO, ZLayer}

import javax.sql.DataSource

case class DataBaseConfig(
    user: String,
    password: String,
    host: String,
    name: String,
    port: Int,
    migrationEnable: Boolean,
) derives ConfigReader {
  def url: String = s"jdbc:postgresql://$host:$port/$name"
  def dataSource: DataSource = {
    val ds = new PGSimpleDataSource()
    ds.setURL(url)
    ds.setUser(user)
    ds.setPassword(password)
    ds
  }
  def transactor: Transactor[Task] =
    Transactor.fromDataSource[Task](dataSource, ExecutionContexts.synchronous)
}

object DataBaseConfig {

  val config: Task[DataBaseConfig] = ZIO.attempt(ConfigSource.default.at(s"db").loadOrThrow[DataBaseConfig])

  val lZio: ZIO[Any, Throwable, Transactor[Task]] = config.map(_.transactor)

  val configLive: ZLayer[Any, Throwable, DataBaseConfig] = ZLayer.fromZIO(config)

  val live: ZLayer[Any, Throwable, Transactor[Task]] = ZLayer.fromZIO(lZio)
}
