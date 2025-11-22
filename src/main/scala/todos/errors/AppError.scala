package todos.errors

/**
 * Иерархия ошибок приложения
 */
sealed trait AppError extends Throwable {
		def message: String
		def code: String
		override def getMessage: String = message
}

object AppErrors {

		// ============ VALIDATION ERRORS ============
		sealed trait ValidationError extends AppError

		case class EmptyFieldError() extends ValidationError {
				override def message: String = s"At least one field must be provided for update"
				override def code: String = "VALIDATION_001"
		}

		case class FieldLengthError(fieldName: String, maxLength: Int, actualLength: Int) extends ValidationError {
				override def message: String = s"Field '$fieldName' cannot exceed $maxLength characters (got $actualLength)"
				override def code: String = "VALIDATION_002"
		}

		case class InvalidDateError(fieldName: String, reason: String) extends ValidationError {
				override def message: String = s"Invalid date for field '$fieldName': $reason"
				override def code: String = "VALIDATION_003"
		}

		case class InvalidFormatError(fieldName: String, expectedFormat: String) extends ValidationError {
				override def message: String = s"Field '$fieldName' has invalid format. Expected: $expectedFormat"
				override def code: String = "VALIDATION_004"
		}

		case class InvalidUUIDError(fieldName: String, value: String) extends ValidationError {
				override def message: String = s"Field '$fieldName' must be a valid UUID (got: $value)"
				override def code: String = "VALIDATION_005"
		}

		// ============ NOT FOUND ERRORS ============
		sealed trait NotFoundError extends AppError

		case class TodoNotFoundError(todoId: String) extends NotFoundError {
				override def message: String = s"Todo with id $todoId not found"
				override def code: String = "NOT_FOUND_001"
		}

		case class UserNotFoundError(userId: String) extends NotFoundError {
				override def message: String = s"User with id $userId not found"
				override def code: String = "NOT_FOUND_002"
		}

		// ============ DATABASE ERRORS ============
		sealed trait DatabaseError extends AppError

		case class DatabaseOperationError(operation: String, cause: Throwable) extends DatabaseError {
				override def message: String = s"Database error during $operation: ${cause.getMessage}"

				override def code: String = "DB_001"
		}

}