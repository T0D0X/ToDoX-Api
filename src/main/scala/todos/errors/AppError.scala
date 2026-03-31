package todos.errors

sealed trait AppError extends Throwable {
  def message: String
  def code: String
  override def getMessage: String = message
}

object AppErrors {

  // ============ VALIDATION ERRORS ============
  sealed trait ValidationErrorBase extends AppError

  case class EmptyFieldError() extends ValidationErrorBase {
    override def message: String = s"At least one field must be provided for update"
    override def code: String = "VALIDATION_001"
  }

  // ============ NOT FOUND ERRORS ============
  sealed trait NotFoundErrorBase extends AppError

  case class TodoNotFoundError(todoId: String) extends NotFoundErrorBase {
    override def message: String = s"Todo with id $todoId not found"
    override def code: String = "NOT_FOUND_001"
  }

  case class RequestNotFoundError(message: String) extends NotFoundErrorBase {
    override def getMessage: String = "Request Error"
    override def code: String = "NOT_FOUND_003"
  }

  // ============ DATABASE ERRORS ============
  sealed trait DatabaseErrorBase extends AppError

}
