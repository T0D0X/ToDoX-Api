package todos.errors

import zio.json._

/**
 * Unified error response for API
 */
case class ErrorResponse(
                        error: String,
                        code: String,
                        message: String,
                        details: Option[String] = None,
                        timestamp: Long = System.currentTimeMillis(),
                        requestId: Option[String] = None
                        )

object ErrorResponse {
		implicit val encoder: JsonEncoder[ErrorResponse] = DeriveJsonEncoder.gen[ErrorResponse]
		
		def fromAppError(error: AppError, details: Option[String] = None, requestId: Option[String] = None): ErrorResponse = {
				ErrorResponse(
						error = error.getClass.getSimpleName.replace("$", ""),
						code = error.code,
						message = error.message,
						details = details,
						requestId = requestId
				)
		}

		// Validation errors
		def validationError(message: String, details: Option[String] = None): ErrorResponse =
				ErrorResponse("ValidationError", "VALIDATION_ERROR", message, details)

		def businessError(message: String, details: Option[String] = None): ErrorResponse =
				ErrorResponse("BusinessRuleViolation", "BUSINESS_ERROR", message, details)

		def notFound(message: String, details: Option[String] = None): ErrorResponse =
				ErrorResponse("NotFoundError", "NOT_FOUND", message, details)

		def unauthorized(message: String, details: Option[String] = None): ErrorResponse =
				ErrorResponse("UnauthorizedError", "UNAUTHORIZED", message, details)

		def forbidden(message: String, details: Option[String] = None): ErrorResponse =
				ErrorResponse("ForbiddenError", "FORBIDDEN", message, details)

		def internalError(message: String, details: Option[String] = None): ErrorResponse =
				ErrorResponse("InternalServerError", "INTERNAL_ERROR", message, details)

		def databaseError(message: String, details: Option[String] = None): ErrorResponse =
				ErrorResponse("RepositoryError", "DATABASE_ERROR", message, details)

		def externalServiceError(message: String, details: Option[String] = None): ErrorResponse =
				ErrorResponse("ExternalServiceError", "EXTERNAL_SERVICE_ERROR", message, details)
}