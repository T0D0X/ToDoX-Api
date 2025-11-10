package todos.models

import zio.json.{DeriveJsonCodec, JsonCodec}

import java.time.Instant

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
}

case class CreateTodoRequest(
                            description: Option[String],
                            priority: Priority,
                            completeAt: Option[Instant],
                            tags: List[String]
                            )
object CreateTodoRequest {
		implicit val codec: JsonCodec[CreateTodoRequest] = DeriveJsonCodec.gen[CreateTodoRequest]
		
		def example =
				CreateTodoRequest(
						description = Some("Study ZIO effects and error handling"),
						priority = Priority.High,
						completeAt = None,
						tags = List("Name")
				)
}