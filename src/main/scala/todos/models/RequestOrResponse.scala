package todos.models

import zio.json.{JsonDecoder, JsonEncoder}

import java.time.Instant
import java.util.UUID

case class UpdateTodoRequest(
    description: Option[String],
    priority: Option[Priority],
    isComplete: Option[Boolean],
    completeAt: Option[Instant],
    tags: Option[List[String]],
) derives JsonDecoder,
      JsonEncoder

object UpdateTodoRequest {
  def empty = UpdateTodoRequest(None, None, None, None, None)
}

case class UserIdOrLogin(
    userId: Option[UUID],
    login: Option[String],
) derives JsonDecoder,
      JsonEncoder

object UserIdOrLogin {
  def empty = UserIdOrLogin(None, None)
}

case class UserIdOrLoginRequest(
    userId: Option[UUID],
    login: Option[String],
) derives JsonDecoder,
      JsonEncoder

case class UpdateUserDataRequest(
    userId: Option[UUID],
    login: Option[String],
    email: Option[String],
    phone: Option[String],
) derives JsonDecoder,
      JsonEncoder
object UpdateUserDataRequest {
  def empty = UpdateUserDataRequest(None, None, None, None)
}

case class CreateTodoRequest(
    userId: UUID,
    description: Option[String],
    priority: Priority,
    completeAt: Option[Instant],
    tags: List[String],
) derives JsonDecoder,
      JsonEncoder {
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

case class RegisterRequest(
    login: String,
    email: String,
    phone: String,
    password: String,
) derives JsonDecoder,
      JsonEncoder

case class JwtResponse(
    token: String,
    user: UserResponse,
    expiresAt: Long,
) derives JsonDecoder,
      JsonEncoder
