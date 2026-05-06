package todos.controller

import sttp.tapir.endpoint
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.server.ServerEndpoint
import todos.errors.AppErrors.TodoNotFoundError
import todos.errors.ErrorResponse.*
import sttp.tapir.ztapir.*
import todos.models.{CreateTodoRequest, TodoItem, UpdateTodoRequest}
import todos.service.{JwtService, TodoService}
import todos.util.EndpointSupport.{standardErrorOut, toErrorResponse}
import zio.{ZIO, ZLayer}

import java.util.UUID
import java.time.Instant

class TodoController(todoService: TodoService, jwtService: JwtService) {

  private val baseEndpoint = endpoint
    .tag("todos")
    .in("api" / "v1" / "todos")
    .securityIn(auth.bearer[String]())
    .errorOut(standardErrorOut)
    .zServerSecurityLogic { token =>
      jwtService
        .validateToken(token)
        .flatMap(value => ZIO.succeed(value))
        .mapError(toErrorResponse)
    }

  // GET /api/v1/todos/{id}
  private val getTodoEndpoint = baseEndpoint.get
    .in(path[UUID]("id"))
    .out(jsonBody[TodoItem])
    .description("Get todo item by ID")
    .serverLogic { _ => id =>
      todoService
        .get(id)
        .someOrFail(TodoNotFoundError(id.toString))
        .mapError(toErrorResponse)
    }

  // DELETE /api/v1/todos/delete/{id}
  private val deleteTodoEndpoint = baseEndpoint.delete
    .in("delete")
    .in(path[UUID]("id"))
    .out(emptyOutput)
    .description("Delete todo item")
    .serverLogic { _ => id =>
      todoService
        .delete(id)
        .mapError(toErrorResponse)
    }

  // POST /api/v1/todos/create
  private val createTodoEndpoint = baseEndpoint.post
    .in("create")
    .in(jsonBody[CreateTodoRequest])
    .out(emptyOutput)
    .description("Create new todo item")
    .serverLogic { _ => createRequest =>
      todoService
        .create(createRequest)
        .mapError(toErrorResponse)
    }

  // PUT /api/v1/todos/update/{id}
  private val updateTodoEndpoint = baseEndpoint.put
    .in("update")
    .in(path[UUID]("id"))
    .in(jsonBody[UpdateTodoRequest])
    .out(emptyOutput)
    .description("Update todo item")
    .serverLogic { _ => (id, updateRequest) =>
      todoService
        .update(id, updateRequest)
        .mapError(toErrorResponse)
    }

  // GET /api/v1/todos/user
  private val getByUserIdTodoEndpoint = baseEndpoint.get
    .in("user")
    .out(jsonBody[List[TodoItem]])
    .description("Get all Todos by UserId")
    .serverLogic { userId => _ =>
      todoService
        .getByUserId(userId)
        .mapError(toErrorResponse)
    }

  val allEndpoints = List(
    getTodoEndpoint,
    updateTodoEndpoint,
    deleteTodoEndpoint,
    createTodoEndpoint,
    getByUserIdTodoEndpoint,
  )
}

object TodoController {
  val live = ZLayer.fromFunction(new TodoController(_, _))
}
