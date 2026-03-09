package todos.config

import org.postgresql.ds.PGSimpleDataSource
import zio.{ZIO, ZLayer}

import javax.sql.DataSource

object DataSourceConfig {
  val layer: ZLayer[DatabaseConfig, Throwable, DataSource] =
    ZLayer.fromZIO {
      for {
        cfg <- ZIO.service[DatabaseConfig]
        ds = new PGSimpleDataSource()
        _  = ds.setURL(s"jdbc:postgresql://${cfg.host}:${cfg.port}/${cfg.name}")
        _  = ds.setUser(cfg.user)
        _  = ds.setPassword(cfg.password)
      } yield ds
    }
}
