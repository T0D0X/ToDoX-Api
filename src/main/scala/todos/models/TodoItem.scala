package todos.models

import zio.json.{JsonDecoder, JsonEncoder}
import zio.schema.{derived, Schema}

import java.time.Instant
import java.util.UUID

case class TodoItem(
    userId: UUID,
    id: UUID,
    description: Option[String],
    priority: Priority,
    isComplete: Boolean,
    createAt: Instant,
    completeAt: Option[Instant],
    tags: List[String],
) derives JsonDecoder,
      JsonEncoder,
      Schema
