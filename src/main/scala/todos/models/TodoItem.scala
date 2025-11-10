package todos.models

import zio.json.{DeriveJsonCodec, JsonCodec}

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
                   tags: List[String]
                   )

object TodoItem {
		implicit val codec: JsonCodec[TodoItem] = DeriveJsonCodec.gen[TodoItem]
}

