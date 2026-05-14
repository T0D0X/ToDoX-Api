package todos.service

import zio.{Task, ZIO, ZLayer}
import org.flywaydb.core.Flyway
import todos.config.DataBaseConfig

trait MigrationService {
  def migrate: Task[Unit]
}

class PostgresMigrationService(config: DataBaseConfig) extends MigrationService {
  val baseText = "PostgreSQL migration"
  override def migrate: Task[Unit] =
    ZIO.ifZIO(ZIO.succeed(config.migrationEnable))(
      onTrue = ZIO.logInfo(s"Start $baseText") *>
        ZIO
          .attemptBlocking {
            Flyway
              .configure()
              .dataSource(config.dataSource)
              .load()
              .migrate()
          }
          .tapBoth(
            error => ZIO.logError(s"$baseText failed: ${error.getMessage}"),
            content => ZIO.logInfo(s"$baseText completed successfully. List update: ${content.migrations}"),
          )
          .unit,
      onFalse = ZIO.logInfo(s"$baseText are disabled in the configuration."),
    )
}

object MigrationService {
  val live = ZLayer.fromFunction(new PostgresMigrationService(_))
}
