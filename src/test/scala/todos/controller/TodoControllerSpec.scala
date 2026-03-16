package todos.controller

import zio.*
import zio.test.*
import zio.http.*
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import todos.errors.AppErrors.*
import todos.models.*
import todos.service.TodoService

import java.time.Instant
import java.util.UUID

object TodoControllerSpec extends ZIOSpecDefault {

  protected val testId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
  protected val testTodo = TodoItem(
    userId = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
    id = testId,
    description = Some("Test description"),
    priority = Priority.Low,
    isComplete = false,
    createAt = Instant.now(),
    completeAt = Some(Instant.now()),
    tags = List.empty,
  )

  def spec = suite("TodoControllerSpec")(
    suite("GET /api/v1/todos/get/{id}")(
      test("return  200 and todo found") {
        val app = createApp(mockService(getResult = ZIO.some(testTodo)))
        val request = Request.get(
          decodeUrl(s"api/v1/todos/get/${testId.toString}"),
        )

        for {
          response <- app(request)
          body <- response.body.asString
        } yield assertTrue(
          response.status == Status.Ok,
          body.contains(testId.toString),
        )
      },
      test("return 404 todo not found") {
        val app = createApp(mockService(getResult = ZIO.none))
        val request = Request.get(
          decodeUrl(s"api/v1/todos/get/${testId.toString}"),
        )

        for {
          response <- app(request)
        } yield assertTrue(
          response.status == Status.NotFound,
        )
      },
      test("return 400 no validate userId") {
        val app = createApp(mockService())
        val request = Request.get(
          decodeUrl(s"api/v1/todos/get/unknown"),
        )

        for {
          response <- app(request)
          body <- response.body.asString.option
        } yield assertTrue(
          response.status == Status.BadRequest,
        )
      },
      test("return 500 database failed") {
        val app = createApp(mockService(getResult = ZIO.fail(new RuntimeException("Database connection failed"))))
        val request = Request.get(
          decodeUrl(s"api/v1/todos/get/${testId.toString}"),
        )

        for {
          response <- app(request)
          body <- response.body.asString.option
        } yield assertTrue(
          response.status == Status.InternalServerError,
          body.isDefined,
          body.exists(_.contains("DatabaseError")),
        )
      },
    ),
    suite("POST /api/v1/todos/create")(
      test("return 200 todo create") {
        val app = createApp(mockService(createResult = ZIO.unit))
        val request = Request.post(
          decodeUrl(s"api/v1/todos/create"),
          Body.fromString(
            """{
              |  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              |  "description": "string",
              |  "priority": "low",
              |  "completeAt": "2025-12-14T07:22:02.250Z",
              |  "tags": [
              |    "string"
              |  ]
              |}""".stripMargin,
          ),
        )

        for {
          response <- app(request)
        } yield assertTrue(
          response.status == Status.Ok,
        )
      },
      test("return 400 JSON no validate") {
        val app = createApp(mockService())
        val request = Request.post(
          decodeUrl(s"api/v1/todos/create"),
          Body.fromString("""{invalid json}"""),
        )

        for {
          response <- app(request)
        } yield assertTrue(
          response.status == Status.BadRequest,
        )
      },
    ),
    suite("PUT /api/v1/todos/update/{id}")(
      test("return 200 validate success") {
        val app = createApp(mockService(updateResult = ZIO.unit))
        val request = Request.put(
          decodeUrl(s"api/v1/todos/update/${testId.toString}"),
          Body.fromString(
            """{"title":"Updated","description":"Updated Desc","isDone":true}""",
          ),
        )

        for {
          response <- app(request)
        } yield assertTrue(
          response.status == Status.Ok,
        )
      },
      test("return 404 todo not found") {
        val app = createApp(mockService(updateResult = ZIO.fail(TodoNotFoundError(testId.toString))))
        val request = Request.put(
          decodeUrl(s"api/v1/todos/update/${testId.toString}"),
          Body.fromString("""{"title":"Updated"}"""),
        )

        for {
          response <- app(request)
          body <- response.body.asString.option
        } yield assertTrue(
          response.status == Status.NotFound,
          body.exists(_.contains("NOT_FOUND_001")),
          body.exists(_.contains(s"Todo with id $testId not found")),
        )
      },
    ),
    suite("DELETE /api/v1/todos/delete/{id}")(
      test("return 200 delete success") {

        val app = createApp(mockService(deleteResult = ZIO.unit))
        val request = Request.delete(
          decodeUrl(s"api/v1/todos/delete/${testId.toString}"),
        )

        for {
          response <- app(request)
        } yield assertTrue(
          response.status == Status.Ok,
        )
      },
      test("return 404 todo not found") {
        val app = createApp(mockService(deleteResult = ZIO.fail(TodoNotFoundError(testId.toString))))
        val request = Request.delete(
          decodeUrl(s"api/v1/todos/delete/${testId.toString}"),
        )

        for {
          response <- app(request)
          body <- response.body.asString.option
        } yield assertTrue(
          response.status == Status.NotFound,
          body.exists(_.contains("NOT_FOUND_001")),
          body.exists(_.contains(s"Todo with id $testId not found")),
        )
      },
    ),
    suite("GET /api/v1/todos/user/{id}")(
      test("return 200 success operation") {
        val app = createApp(mockService(getByUserIdResult = ZIO.succeed(List(testTodo))))
        val request = Request.get(
          decodeUrl(s"api/v1/todos/user/${testTodo.userId}"),
        )

        for {
          response <- app(request)
        } yield assertTrue(
          response.status == Status.Ok,
        )
      },
      // Todo: после реализации проверки есть ли такой пользователь добавить тест
    ),
  )

  private def mockService(
      getResult: Task[Option[TodoItem]] = ZIO.none,
      createResult: Task[Unit] = ZIO.unit,
      updateResult: Task[Unit] = ZIO.unit,
      deleteResult: Task[Unit] = ZIO.unit,
      getByUserIdResult: Task[List[TodoItem]] = ZIO.succeed(List.empty[TodoItem]),
  ): TodoService = new TodoService {
    override def get(id: UUID): Task[Option[TodoItem]] = getResult

    override def create(request: CreateTodoRequest): Task[Unit] = createResult

    override def update(id: UUID, request: UpdateTodoRequest): Task[Unit] =
      updateResult

    override def delete(id: UUID): Task[Unit] = deleteResult

    override def getByUserId(userId: UUID): Task[List[TodoItem]] =
      getByUserIdResult
  }

  private def decodeUrl(path: String): URL =
    URL.decode(path).getOrElse {
      throw new IllegalArgumentException(s"Invalid URL path: $path")
    }

  private def createApp(service: TodoService): Routes[Any, Response] = {
    val controller = new TodoController(service)
    ZioHttpInterpreter().toHttp(controller.allEndpoints)
  }
}
