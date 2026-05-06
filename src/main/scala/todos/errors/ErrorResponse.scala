package todos.errors

import todos.errors.AppErrors
import zio.json.{JsonDecoder, JsonEncoder}
import zio.schema.{derived, Schema}

import java.time.Instant

/** Error response for API
  */
case class ErrorResponse(
    error: String,
    code: String,
    message: String,
    timestamp: Instant = Instant.now(),
) derives JsonDecoder,
      JsonEncoder,
      Schema

object ErrorResponse {

  def fromAppError(error: AppError): ErrorResponse =
    error match {
      case v: AppErrors.ValidationErrorBase =>
        ErrorResponse(
          error = "ValidationError",
          code = v.code,
          message = v.getMessage,
        )

      case n: AppErrors.NotFoundErrorBase =>
        ErrorResponse(
          error = "NotFoundError",
          code = n.code,
          message = n.getMessage,
        )
      case j: AppErrors.AuthErrorBase =>
        ErrorResponse(
          error = "AuthError",
          code = j.code,
          message = j.getMessage,
        )

      case d: AppErrors.DatabaseErrorBase =>
        ErrorResponse(
          error = "DatabaseError",
          code = d.code,
          message = d.getMessage,
        )
    }
}
