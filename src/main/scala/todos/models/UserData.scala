package todos.models

import zio.json.{JsonDecoder, JsonEncoder}
import zio.schema.{derived, Schema}
import io.circe.{Decoder, Encoder}

import java.util.UUID

case class UserData(
    userId: UUID,
    login: String,
    email: String,
    phone: String,
    passwordHash: String,
) derives Decoder,
      Encoder {
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
      JsonEncoder,
      Schema
