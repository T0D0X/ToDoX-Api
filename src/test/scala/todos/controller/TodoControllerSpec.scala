package todos.controller

import zio.*
import zio.test.*
import zio.http.*
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import todos.errors.ErrorResponse
import todos.models.*
import todos.common.{MockService, TestRequests}
import todos.errors.AppErrors.{EmptyFieldError, TodoNotFoundError}
import todos.service.{JwtService, TodoService}
import zio.json.*

import java.time.Instant
import java.util.UUID

object TodoControllerSpec extends ZIOSpecDefault {

  private val testId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
  private val testTodo = TodoItem(
    userId = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
    id = testId,
    description = Some("Test description"),
    priority = Priority.Low,
    isComplete = false,
    createAt = Instant.now(),
    completeAt = Some(Instant.now()),
    tags = List.empty,
  )
  private val createTodoRequest = CreateTodoRequest(
    userId = testTodo.userId,
    description = testTodo.description,
    priority = testTodo.priority,
    completeAt = testTodo.completeAt,
    tags = testTodo.tags,
  )

  private val updateTodoRequest = UpdateTodoRequest(
    description = Some("Update text"),
    priority = Some(Priority.Medium),
    isComplete = Some(true),
    completeAt = None,
    tags = None,
  )

  val tokenValid = "token-test"
  val tokenInvalid = "incorrect"

  def spec = suite("TodoControllerSpec")(
    suite("GET /api/v1/todos/get/{id}")(
      test("return  200 and todo found") {
        val app = makeApp(
          MockService.mockTodoService(getResult = ZIO.some(testTodo)),
          MockService.mockJwtService(Some(testTodo.userId)),
        )
        val request = TestRequests.get(s"api/v1/todos/${testTodo.userId.toString}", tokenValid)

        for {
          response <- app(request)
          body <- response.body.asJson[TodoItem]
        } yield assertTrue(
          response.status == Status.Ok,
          body == testTodo,
        )
      },
      test("return 404 todo not found") {
        val app = makeApp(
          MockService.mockTodoService(getResult = ZIO.none),
          MockService.mockJwtService(Some(testTodo.userId)),
        )
        val request = TestRequests.get(s"api/v1/todos/${testTodo.userId.toString}", tokenValid)

        for {
          response <- app(request)
          body <- response.body.asJson[ErrorResponse]
        } yield assertTrue(
          response.status == Status.NotFound,
          body.error == "NotFoundError",
          body.code == "NOT_FOUND_001",
          body.message == s"Todo with id ${testTodo.userId} not found",
        )
      },
      test("return 400 no validate userId") {
        val app = makeApp(
          MockService.mockTodoService(),
          MockService.mockJwtService(Some(testTodo.userId)),
        )
        val request = TestRequests.get("api/v1/todos/unknown", tokenValid)

        for {
          response <- app(request)
        } yield assertTrue(
          response.status == Status.BadRequest,
        )
      },
      test("return 400 no validate authToken") {
        val app = makeApp(
          MockService.mockTodoService(getResult = ZIO.some(testTodo)),
          MockService.mockJwtService(),
        )
        val request = TestRequests.get(s"api/v1/todos/${testTodo.userId.toString}", tokenInvalid)

        for {
          response <- app(request)
          body <- response.body.asJson[ErrorResponse]
        } yield assertTrue(
          response.status == Status.Unauthorized,
          body.error == "AuthError",
          body.code == "AUTH_ERROR_0",
          body.message == s"invalid token $tokenInvalid",
        )
      },
      test("return 500 database failed") {
        val app = makeApp(
          MockService.mockTodoService(getResult = ZIO.fail(new RuntimeException("Database connection failed"))),
          MockService.mockJwtService(Some(testTodo.userId)),
        )
        val request = TestRequests.get(s"api/v1/todos/${testTodo.userId.toString}", tokenValid)

        for {
          response <- app(request)
          body <- response.body.asJson[ErrorResponse]
        } yield assertTrue(
          response.status == Status.BadRequest,
          body.error == "InternalError",
          body.code == "UNKNOWN",
          body.message == "Database connection failed",
        )
      },
      test("return 401") {
        val app = makeApp(
          MockService.mockTodoService(),
          MockService.mockJwtService(),
        )
        val request = TestRequests.get(s"api/v1/todos/${testTodo.userId.toString}", tokenInvalid)

        for {
          response <- app(request)
          body <- response.body.asJson[ErrorResponse]
        } yield assertTrue(
          response.status == Status.Unauthorized,
          body.error == "AuthError",
          body.code == "AUTH_ERROR_0",
          body.message == "invalid token incorrect",
        )
      },
    ),
    suite("POST /api/v1/todos/create")(
      test("return 200 todo create") {
        val app = makeApp(
          MockService.mockTodoService(createResult = ZIO.unit),
          MockService.mockJwtService(Some(testTodo.userId)),
        )
        val request = TestRequests.post("api/v1/todos/create", createTodoRequest.toJson, tokenValid)

        for {
          response <- app(request)
        } yield assertTrue(
          response.status == Status.Ok,
          response.body.isEmpty,
        )
      },
      test("return 400 JSON no validate") {
        val app = makeApp(
          MockService.mockTodoService(),
          MockService.mockJwtService(Some(testTodo.userId)),
        )
        val request = TestRequests.post("api/v1/todos/create", "{invalid json}", tokenValid)

        for {
          response <- app(request)
        } yield assertTrue(
          response.status == Status.BadRequest,
        )
      },
      test("return 500 database failed") {
        val app = makeApp(
          MockService.mockTodoService(createResult = ZIO.fail(new RuntimeException("Database connection failed"))),
          MockService.mockJwtService(Some(testTodo.userId)),
        )
        val request = TestRequests.post("api/v1/todos/create", createTodoRequest.toJson, tokenValid)

        for {
          response <- app(request)
          body <- response.body.asJson[ErrorResponse]
        } yield assertTrue(
          response.status == Status.BadRequest,
          body.error == "InternalError",
          body.code == "UNKNOWN",
          body.message == "Database connection failed",
        )
      },
      test("return 401") {
        val app = makeApp(
          MockService.mockTodoService(),
          MockService.mockJwtService(),
        )
        val request = TestRequests.post(
          "api/v1/todos/create",
          createTodoRequest.toJson,
          tokenInvalid,
        )

        for {
          response <- app(request)
          body <- response.body.asJson[ErrorResponse]
        } yield assertTrue(
          response.status == Status.Unauthorized,
          body.error == "AuthError",
          body.code == "AUTH_ERROR_0",
          body.message == "invalid token incorrect",
        )
      },
    ),
    suite("PUT /api/v1/todos/update/{id}")(
      test("return 200 validate success") {
        val app = makeApp(
          MockService.mockTodoService(updateResult = ZIO.unit),
          MockService.mockJwtService(Some(testTodo.userId)),
        )
        val request =
          TestRequests.put(s"api/v1/todos/update/${testTodo.userId.toString}", updateTodoRequest.toJson, tokenValid)

        for {
          response <- app(request)
        } yield assertTrue(
          response.status == Status.Ok,
          response.body.isEmpty,
        )
      },
      test("return validation error") {
        val app = makeApp(
          MockService.mockTodoService(updateResult = ZIO.fail(EmptyFieldError())),
          MockService.mockJwtService(Some(testTodo.userId)),
        )
        val request = TestRequests.put(
          s"api/v1/todos/update/${testTodo.userId.toString}",
          UpdateTodoRequest.empty.toJson,
          tokenValid,
        )

        for {
          response <- app(request)
          body <- response.body.asJson[ErrorResponse]
        } yield assertTrue(
          response.status == Status.UnprocessableEntity,
          body.error == "ValidationError",
          body.code == "VALIDATION_001",
          body.message == "At least one field must be provided for update",
        )
      },
      test("return 404 todo not found") {
        val app = makeApp(
          MockService.mockTodoService(updateResult = ZIO.fail(TodoNotFoundError(testTodo.userId.toString))),
          MockService.mockJwtService(Some(testTodo.userId)),
        )
        val request = TestRequests.put(
          s"api/v1/todos/update/${testTodo.userId.toString}",
          updateTodoRequest.toJson,
          tokenValid,
        )

        for {
          response <- app(request)
          body <- response.body.asJson[ErrorResponse]
        } yield assertTrue(
          response.status == Status.NotFound,
          body.error == "NotFoundError",
          body.code == "NOT_FOUND_001",
          body.message == s"Todo with id ${testTodo.userId} not found",
        )
      },
      test("return 401") {
        val app = makeApp(
          MockService.mockTodoService(updateResult = ZIO.unit),
          MockService.mockJwtService(),
        )
        val request = TestRequests.put(
          s"api/v1/todos/update/${testTodo.userId.toString}",
          updateTodoRequest.toJson,
          tokenInvalid,
        )

        for {
          response <- app(request)
          body <- response.body.asJson[ErrorResponse]
        } yield assertTrue(
          response.status == Status.Unauthorized,
          body.error == "AuthError",
          body.code == "AUTH_ERROR_0",
          body.message == "invalid token incorrect",
        )
      },
    ),
    suite("DELETE /api/v1/todos/delete/{id}")(
      test("return 200 delete success") {

        val app = makeApp(
          MockService.mockTodoService(deleteResult = ZIO.unit),
          MockService.mockJwtService(Some(testTodo.userId)),
        )
        val request = TestRequests.delete(s"api/v1/todos/delete/${testTodo.userId.toString}", tokenValid)

        for {
          response <- app(request)
        } yield assertTrue(
          response.status == Status.Ok,
          response.body.isEmpty,
        )
      },
      test("return 404 todo not found") {
        val app = makeApp(
          MockService.mockTodoService(deleteResult = ZIO.fail(TodoNotFoundError(testTodo.userId.toString))),
          MockService.mockJwtService(Some(testTodo.userId)),
        )
        val request = TestRequests.delete(s"api/v1/todos/delete/${testTodo.userId.toString}", tokenValid)

        for {
          response <- app(request)
          body <- response.body.asJson[ErrorResponse]
        } yield assertTrue(
          response.status == Status.NotFound,
          body.error == "NotFoundError",
          body.code == "NOT_FOUND_001",
          body.message == s"Todo with id ${testTodo.userId} not found",
        )
      },
      test("return 401") {
        val app = makeApp(
          MockService.mockTodoService(deleteResult = ZIO.unit),
          MockService.mockJwtService(),
        )
        val request = TestRequests.delete(s"api/v1/todos/delete/${testTodo.userId.toString}", tokenInvalid)

        for {
          response <- app(request)
          body <- response.body.asJson[ErrorResponse]
        } yield assertTrue(
          response.status == Status.Unauthorized,
          body.error == "AuthError",
          body.code == "AUTH_ERROR_0",
          body.message == "invalid token incorrect",
        )
      },
    ),
    suite("GET /api/v1/todos/user/all")(
      test("return 200 success operation") {
        val app = makeApp(
          MockService.mockTodoService(getByUserIdResult = ZIO.succeed(List(testTodo))),
          MockService.mockJwtService(Some(testTodo.userId)),
        )
        val request = TestRequests.get("api/v1/todos/user/all", tokenValid)

        for {
          response <- app(request)
          body <- response.body.asJson[List[TodoItem]]
        } yield assertTrue(
          response.status == Status.Ok,
          body.size == 1,
          body.contains(testTodo),
        )
      },
      test("return 500 database failed") {
        val app =
          makeApp(
            MockService.mockTodoService(getByUserIdResult =
              ZIO.fail(new RuntimeException("Database connection failed")),
            ),
            MockService.mockJwtService(Some(testTodo.userId)),
          )
        val request = TestRequests.get("api/v1/todos/user/all", tokenValid)

        for {
          response <- app(request)
          body <- response.body.asJson[ErrorResponse]
        } yield assertTrue(
          response.status == Status.BadRequest,
          body.error == "InternalError",
          body.code == "UNKNOWN",
          body.message == "Database connection failed",
        )
      },
      test("return 401") {
        val app =
          makeApp(
            MockService.mockTodoService(getByUserIdResult =
              ZIO.fail(new RuntimeException("Database connection failed")),
            ),
            MockService.mockJwtService(),
          )
        val request = TestRequests.get("api/v1/todos/user/all", tokenInvalid)

        for {
          response <- app(request)
          body <- response.body.asJson[ErrorResponse]
        } yield assertTrue(
          response.status == Status.Unauthorized,
          body.error == "AuthError",
          body.code == "AUTH_ERROR_0",
          body.message == "invalid token incorrect",
        )
      },
    ),
  )

  private def makeApp(
      todoService: TodoService,
      jwtService: JwtService,
  ): Routes[Any, Response] = {
    val controller = new TodoController(todoService, jwtService)
    ZioHttpInterpreter().toHttp(controller.allEndpoints)
  }
}
