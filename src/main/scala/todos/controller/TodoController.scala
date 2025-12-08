package todos.controller

import sttp.tapir.endpoint
import sttp.tapir.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.server.ServerEndpoint
import todos.errors.AppError
import todos.errors.AppErrors.*
import sttp.tapir.ztapir.*
import todos.models.{CreateTodoRequest, TodoItem, UpdateTodoRequest}
import todos.service.TodoService

import java.util.UUID

class TodoController(todoService: TodoService) {

		val allEndpoints: List[ZServerEndpoint[Nothing, Any]] = List(
				getTodoEndpoint,
				updateTodoEndpoint,
				deleteTodoEndpoint,
				createTodoEndpoint,
		)

		// GET /api/v1/todos/get/{id}
		val getTodoEndpoint = baseEndpoint
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
						case ex => DatabaseOperationError("get")
				}
		}

		// DELETE /api/v1/todos/delete/{id}
		val deleteTodoEndpoint = baseEndpoint
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
						case ex: Throwable => DatabaseOperationError("delete")
				}
		}

		// POST /api/v1/todos/create
		val createTodoEndpoint = baseEndpoint
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
						case ex: Throwable => DatabaseOperationError("create")
				}
		}
		// PUT /api/v1/todos/update/{id}
		private val updateTodoEndpoint = baseEndpoint
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
						case ex: Throwable => DatabaseOperationError("update")
				}
		}

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
}

object TodoController {
		def make(todoService: TodoService): TodoController =
				new TodoController(todoService)
}
