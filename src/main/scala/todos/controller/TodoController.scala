package todos.controller

import sttp.tapir.endpoint
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.json.zio.jsonBody
import todos.errors.AppError
import todos.errors.AppErrors.*
import todos.models.{TodoItem, UpdateTodoRequest, CreateTodoRequest}
import todos.service.TodoService
import zio.ZIO

import java.util.UUID

class TodoController(todoService: TodoService) {
		implicit class EndpointOps[I, E, O](val endpoint: PublicEndpoint[I, E, O, Any]) {
				def withServerLogic(logic: I => ZIO[Any, Throwable, Either[E, O]]): PublicEndpoint[I, E, O, Any] = {
						endpoint.serverLogic(logic)
						endpoint
				}
		}

		val allEndpoints: List[AnyEndpoint] = List(
				getTodoEndpoint,
				updateTodoEndpoint,
				deleteTodoEndpoint,
				createTodoEndpoint,
		)

		// GET /api/v1/todos/get/{id}
		private val getTodoEndpoint = baseEndpoint
		.get
		.in("get")
		.in(path[UUID]("id"))
		.out(jsonBody[TodoItem])
		.description("Get todo item by ID")
		.withServerLogic { id =>
				todoService.get(id)
				.map {
						case Some(todo) => Right(todo)
						case None => Left(TodoNotFoundError(id.toString))
				}
				.catchAll {
						case ex: Throwable => ZIO.succeed(Left(DatabaseOperationError("get")))
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
		.withServerLogic { case (id, updateRequest) =>
				todoService.update(id, updateRequest)
				.as(Right(()))
				.catchAll {
						case ex: AppError => ZIO.succeed(Left(ex))
						case ex: Throwable => ZIO.succeed(Left(DatabaseOperationError("update")))
				}
		}

		// DELETE /api/v1/todos/delete/{id}
		private val deleteTodoEndpoint = baseEndpoint
		.delete
		.in("delete")
		.in(path[UUID]("id"))
		.out(emptyOutput)
		.description("Delete todo item")
		.withServerLogic { id =>
				todoService.delete(id)
				.as(Right(()))
				.catchAll {
						case ex: AppError => ZIO.succeed(Left(ex))
						case ex: Throwable => ZIO.succeed(Left(DatabaseOperationError("delete")))
				}
		}

		// POST /api/v1/todos/create
		private val createTodoEndpoint = baseEndpoint
		.post
		.in("create")
		.in(jsonBody[CreateTodoRequest])
		.out(emptyOutput)
		.description("Create new todo item")
		.withServerLogic { createRequest =>
				todoService.create(createRequest)
				.as(Right(()))
				.catchAll {
						case ex: AppError => ZIO.succeed(Left(ex))
						case ex: Throwable => ZIO.succeed(Left(DatabaseOperationError("create")))
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

object TodoEndpoints {
		def make(todoService: TodoService): TodoController =
				new TodoController(todoService)
}
