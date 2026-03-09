package todos.service

import org.flywaydb.core.Flyway
import scala.jdk.CollectionConverters.*
import todos.models.{ExecutedMigration, MigrationResult}
import zio.*

import javax.sql.DataSource

trait JobsService {
  def migrate: Task[MigrationResult]
}

final class JobsServiceImpl(dataSource: DataSource) extends JobsService {
  override def migrate: Task[MigrationResult] = ZIO.attemptBlocking {
    val flyway = Flyway.configure().dataSource(dataSource).load()
    val result = flyway.migrate()
    MigrationResult(
      executedMigrations = result.migrationsExecuted,
      initialSchemaVersion = result.initialSchemaVersion,
      targetSchemaVersion = result.targetSchemaVersion,
      migrations = result.migrations.asScala.map { m =>
        ExecutedMigration(
          version = m.version,
          description = m.description,
          script = m.filepath,
          executionTimeMs = m.executionTime,
          success = m.rolledBack
        )
      }.toList
    )
  }
}

object JobsServiceImpl {
  def make(dataSource: DataSource): JobsService =
    new JobsServiceImpl(dataSource)
}
