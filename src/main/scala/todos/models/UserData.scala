package todos.models

import zio.json.{JsonDecoder, JsonEncoder}

import java.util.UUID

case class UserData(
    userId: UUID,
    login: String,
    email: String,
    phone: String,
    passwordHash: String,
) {
  def toResponse: UserResponse =
    UserResponse(
      userId = userId,
      login = login,
      email = email,
      phone = phone,
    )
}

case class UserResponse(
    userId: UUID,
    login: String,
    email: String,
    phone: String,
) derives JsonDecoder,
      JsonEncoder
