package todos.controller

import sttp.tapir.endpoint
import sttp.tapir.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.json.zio.jsonBody
import todos.errors.AppError
import todos.errors.AppErrors.*
import sttp.tapir.ztapir.*
import todos.models.{CreateTodoRequest, TodoItem, UpdateTodoRequest}
import todos.service.TodoService

import java.util.UUID

class TodoController(todoService: TodoService) {

		private val baseEndpoint = endpoint
		.tag("todos")
		.in("api" / "v1" / "todos")
		.errorOut(
				oneOf[AppError](
						oneOfVariant(StatusCode.NotFound, jsonBody[NotFoundErrorBase].description("Not found")),
						oneOfVariant(StatusCode.UnprocessableEntity, jsonBody[ValidationErrorBase].description("There is a logical error in the data.")),
						oneOfVariant(StatusCode.InternalServerError, jsonBody[DatabaseErrorBase].description("Database error")),
				)
		)

		val allEndpoints: List[ZServerEndpoint[Any, Any]] = List(
				getTodoEndpoint,
				updateTodoEndpoint,
				deleteTodoEndpoint,
				createTodoEndpoint,
		)

		// GET /api/v1/todos/get/{id}
		lazy val getTodoEndpoint: ZServerEndpoint[Any, Any] = baseEndpoint
		.get
		.in("get")
		.in(path[UUID]("id"))
		.out(jsonBody[TodoItem])
		.description("Get todo item by ID")
		.zServerLogic { id =>
				todoService.get(id)
				.someOrFail(TodoNotFoundError(id.toString))
				.mapError {
						case _: TodoNotFoundError => TodoNotFoundError(id.toString)
						case ex => DatabaseOperationError("get", ex.toString)
				}
		}

		// DELETE /api/v1/todos/delete/{id}
		lazy val deleteTodoEndpoint: ZServerEndpoint[Any, Any] = baseEndpoint
		.delete
		.in("delete")
		.in(path[UUID]("id"))
		.out(emptyOutput)
		.description("Delete todo item")
		.zServerLogic { id =>
				todoService.delete(id)
				.unit
				.mapError {
						case ex: AppError => ex
						case ex: Throwable => DatabaseOperationError("delete", ex.toString)
				}
		}

		// POST /api/v1/todos/create
		lazy val createTodoEndpoint: ZServerEndpoint[Any, Any] = baseEndpoint
		.post
		.in("create")
		.in(jsonBody[CreateTodoRequest])
		.out(emptyOutput)
		.description("Create new todo item")
		.zServerLogic { createRequest =>
				todoService.create(createRequest)
				.unit
				.mapError {
						case ex: AppError => ex
						case ex: Throwable => DatabaseOperationError("create", ex.toString)
				}
		}
		// PUT /api/v1/todos/update/{id}
		lazy val updateTodoEndpoint: ZServerEndpoint[Any, Any] = baseEndpoint
		.put
		.in("update")
		.in(path[UUID]("id"))
		.in(jsonBody[UpdateTodoRequest])
		.out(emptyOutput)
		.description("Update todo item")
		.zServerLogic { case (id, updateRequest) =>
				todoService.update(id, updateRequest)
				.unit
				.mapError {
						case ex: AppError => ex
						case ex: Throwable => DatabaseOperationError("update", ex.toString)
				}
		}
}

object TodoController {
		def make(todoService: TodoService): TodoController =
				new TodoController(todoService)
}
