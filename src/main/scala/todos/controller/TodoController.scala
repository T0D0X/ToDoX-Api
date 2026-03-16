package todos.controller

import sttp.tapir.endpoint
import sttp.tapir.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.json.zio.jsonBody
import todos.errors.{AppError, ErrorResponse}
import todos.errors.AppErrors.TodoNotFoundError
import todos.errors.ErrorResponse.*
import sttp.tapir.ztapir.*
import todos.models.{CreateTodoRequest, TodoItem, UpdateTodoRequest}
import todos.service.TodoService

import java.util.UUID
import java.time.Instant

class TodoController(todoService: TodoService) {

  private val notFoundOutput =
    jsonBody[ErrorResponse]
      .description("Ресурс не найден")
      .example(
        ErrorResponse(
          error = "TodoNotFoundError",
          code = "NOT_FOUND",
          message = "Todo with id 123e4567-e89b-12d3-a456-426614174000 not found",
          timestamp = Instant.parse("2021-10-01T12:00:00Z"),
        ),
      )

  private val validationOutput =
    jsonBody[ErrorResponse]
      .description("Ошибка валидации")
      .example(
        ErrorResponse(
          error = "ValidationError",
          code = "VALIDATION",
          message = "Field 'title' cannot be empty",
          timestamp = Instant.parse("2021-10-01T12:00:00Z"),
        ),
      )

  private val databaseOutput =
    jsonBody[ErrorResponse]
      .description("Ошибка базы данных")
      .example(
        ErrorResponse(
          error = "DatabaseError",
          code = "DB",
          message = "Database operation failed",
          timestamp = Instant.parse("2021-10-01T12:00:00Z"),
        ),
      )

  private val baseEndpoint = endpoint
    .tag("todos")
    .in("api" / "v1" / "todos")
    .errorOut(
      oneOf[ErrorResponse](
        oneOfVariantValueMatcher(StatusCode.NotFound, notFoundOutput) {
          case ErrorResponse(_, code, _, _) if code.startsWith("NOT_FOUND") => true
        },
        oneOfVariantValueMatcher(StatusCode.UnprocessableEntity, validationOutput) {
          case ErrorResponse(_, code, _, _) if code.startsWith("VALIDATION") => true
        },
        oneOfVariantValueMatcher(StatusCode.InternalServerError, databaseOutput) {
          case ErrorResponse(_, code, _, _) if code.startsWith("DB") => true
        },
      ),
    )

  private def toErrorResponse(throwable: Throwable): ErrorResponse = throwable match {
    case error: AppError => ErrorResponse.fromAppError(error)
    case ex: Throwable =>
      ErrorResponse(
        error = "DatabaseError",
        code = "DB",
        message = ex.getMessage,
      )
  }

  // GET /api/v1/todos/get/{id}
  val getTodoEndpoint: ZServerEndpoint[Any, Any] = baseEndpoint.get
    .in("get")
    .in(path[UUID]("id"))
    .out(jsonBody[TodoItem])
    .description("Get todo item by ID")
    .zServerLogic { id =>
      todoService
        .get(id)
        .someOrFail(TodoNotFoundError(id.toString))
        .mapError(toErrorResponse)
    }

  // DELETE /api/v1/todos/delete/{id}
  val deleteTodoEndpoint: ZServerEndpoint[Any, Any] = baseEndpoint.delete
    .in("delete")
    .in(path[UUID]("id"))
    .out(emptyOutput)
    .description("Delete todo item")
    .zServerLogic { id =>
      todoService
        .delete(id)
        .mapError(toErrorResponse)
    }

  // POST /api/v1/todos/create
  val createTodoEndpoint: ZServerEndpoint[Any, Any] = baseEndpoint.post
    .in("create")
    .in(jsonBody[CreateTodoRequest])
    .out(emptyOutput)
    .description("Create new todo item")
    .zServerLogic { createRequest =>
      todoService
        .create(createRequest)
        .mapError(toErrorResponse)
    }

  // PUT /api/v1/todos/update/{id}
  val updateTodoEndpoint: ZServerEndpoint[Any, Any] = baseEndpoint.put
    .in("update")
    .in(path[UUID]("id"))
    .in(jsonBody[UpdateTodoRequest])
    .out(emptyOutput)
    .description("Update todo item")
    .zServerLogic { case (id, updateRequest) =>
      todoService
        .update(id, updateRequest)
        .mapError(toErrorResponse)
    }

  // GET /api/v1/todos/user/{id}
  val getByUserIdTodoEndpoint: ZServerEndpoint[Any, Any] = baseEndpoint.get
    .in("user")
    .in(path[UUID]("id"))
    .out(jsonBody[List[TodoItem]])
    .description("Get all Todos by UserId")
    .zServerLogic { userId =>
      todoService
        .getByUserId(userId)
        .mapError(toErrorResponse)
    }

  val allEndpoints: List[ZServerEndpoint[Any, Any]] = List(
    getTodoEndpoint,
    updateTodoEndpoint,
    deleteTodoEndpoint,
    createTodoEndpoint,
    getByUserIdTodoEndpoint,
  )
}

object TodoController {
  def make(todoService: TodoService): TodoController =
    new TodoController(todoService)
}
