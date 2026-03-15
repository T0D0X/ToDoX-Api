package todos.models

import zio.json.{JsonDecoder, JsonEncoder}

import java.util.UUID

case class UserData(
    userId: UUID,
    login: String,
    email: String,
    phone: String
) derives JsonDecoder,
      JsonEncoder
