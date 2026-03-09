package todos.models

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

final case class ExecutedMigration(
    version: String,
    description: String,
    script: String,
    executionTimeMs: Long,
    success: Boolean
)

object ExecutedMigration {
  given JsonEncoder[ExecutedMigration] = DeriveJsonEncoder.gen
  given JsonDecoder[ExecutedMigration] = DeriveJsonDecoder.gen
}

final case class MigrationResult(
    executedMigrations: Int,
    initialSchemaVersion: String,
    targetSchemaVersion: String,
    migrations: List[ExecutedMigration]
)

object MigrationSummary {
  given JsonEncoder[MigrationResult] = DeriveJsonEncoder.gen
  given JsonDecoder[MigrationResult] = DeriveJsonDecoder.gen
}
