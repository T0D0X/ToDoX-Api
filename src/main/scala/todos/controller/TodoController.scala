package todos.controller

import sttp.tapir.endpoint
import todos.service.TodoService
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.server.ServerEndpoint
import todos.errors.AppError
import todos.errors.AppErrors.*
import todos.models.TodoItem
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
		)

		// GET /api/v1/todos/{id} - получить todo по ID
		val getTodoEndpoint = baseEndpoint
		.get
		.in(path[UUID]("id"))
		.out(jsonBody[TodoItem])
		.description("Get todo item by ID")
		.errorOut(
				oneOf[AppError](
						oneOfVariant(StatusCode.NotFound, jsonBody[NotFoundError])
				)
		)
		.withServerLogic { id =>
				todoService.get(id)
				.map {
						case Some(todo) => Right(todo)
						case None => Left(TodoNotFoundError(id.toString))
				}
				.catchAll {
						case ex: Throwable => ZIO.succeed(Left(DatabaseOperationError("get", ex)))
				}
		}

		private val baseEndpoint = endpoint
		.tag("todos")
		.in("api")
		.in("v1")
		.in("todos")

}
