package todos.errors

import zio.json._
import java.time.Instant

/** Unified error response for API
  */
case class ErrorResponse(
    error: String,
    code: String,
    message: String,
    timestamp: Instant = Instant.now()
)

object ErrorResponse {
  implicit val encoder: JsonEncoder[ErrorResponse] =
    DeriveJsonEncoder.gen[ErrorResponse]
  implicit val decoder: JsonDecoder[ErrorResponse] =
    DeriveJsonDecoder.gen[ErrorResponse]

  def fromAppError(error: AppError): ErrorResponse =
    error match {
      case v: AppErrors.ValidationErrorBase =>
        ErrorResponse(
          error = "ValidationError",
          code = v.code,
          message = v.message
        )

      case n: AppErrors.NotFoundErrorBase =>
        ErrorResponse(
          error = "NotFoundError",
          code = n.code,
          message = n.message
        )

      case d: AppErrors.DatabaseErrorBase =>
        ErrorResponse(
          error = "DatabaseError",
          code = d.code,
          message = d.message
        )
    }
}
