package todos.models

import zio.json.{DeriveJsonCodec, JsonCodec}

import java.time.Instant
import java.util.UUID

case class UpdateTodoRequest(
                            description: Option[String],
                            priority: Option[Priority],
                            isComplete: Option[Boolean],
                            completeAt: Option[Instant],
                            tags: Option[List[String]]
                            )

object UpdateTodoRequest {
		implicit val codec: JsonCodec[UpdateTodoRequest] = DeriveJsonCodec.gen[UpdateTodoRequest]
		
		def example =
				UpdateTodoRequest(
						description = Some("Study ZIO effects and error handling"),
						priority = Some(Priority.High),
						isComplete = Some(false),
						completeAt = None,
						tags = Some(List("Name"))
				)

		def empty = UpdateTodoRequest(None, None, None, None, None)
}

case class CreateTodoRequest(
                            userId: UUID,
                            description: Option[String],
                            priority: Priority,
                            completeAt: Option[Instant],
                            tags: List[String]
                            ) {
		def toToDoItem = TodoItem(
				userId = userId,
				id = UUID.randomUUID(),
				description = description,
				priority = priority,
				isComplete = false,
				createAt = Instant.now(),
				completeAt = completeAt,
				tags = tags,
		)
}
object CreateTodoRequest {
		implicit val codec: JsonCodec[CreateTodoRequest] = DeriveJsonCodec.gen[CreateTodoRequest]

		def example =
				CreateTodoRequest(
						userId = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
						description = Some("Study ZIO effects and error handling"),
						priority = Priority.High,
						completeAt = None,
						tags = List("Name")
				)
}