package todos.controller

import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.http.*
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import todos.errors.AppErrors.*
import todos.models.*
import todos.service.TodoService

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

		def spec = suite("TodoController Http Tests")(
				suite("GET /api/v1/todos/get/{id}")(
						test("возвращает 200 и todo при успешном поиске") {
								val mockService = createMockService(
										getResult = ZIO.some(testTodo)
								)

								val app = createApp(mockService)
								val request = Request.get(
										decodeUrl(s"api/v1/todos/get/${testId.toString}")
								)

								for {
										response <- app(request)
										body <- response.body.asString
								} yield assert(response.status)(equalTo(Status.Ok)) &&
								assert(body)(containsString(testId.toString))
						},

						test("возвращает 404 при отсутствии todo") {
								val mockService = createMockService(
										getResult = ZIO.none
								)

								val app = createApp(mockService)
								val request = Request.get(
										decodeUrl(s"api/v1/todos/get/${testId.toString}")
								)

								for {
										response <- app(request)
								} yield assert(response.status)(equalTo(Status.NotFound))
						},

						test("возвращает 500 при ошибке базы данных") {
								val mockService = createMockService(
										getResult = ZIO.fail(new RuntimeException("Database connection failed"))
								)

								val app = createApp(mockService)
								val request = Request.get(
										decodeUrl(s"api/v1/todos/get/${testId.toString}")
								)

								for {
										response <- app(request)
										body <- response.body.asString.option
								} yield assert(response.status)(equalTo(Status.InternalServerError)) &&
								assert(body)(isSome(containsString("DatabaseOperationError")))
						},

						test("валидирует UUID параметр") {
								val mockService = createMockService()

								val app = createApp(mockService)
								val request = Request.get(
										decodeUrl(s"api/v1/todos/get/not-a-uuid")
								)

								for {
										response <- app(request)
								} yield assert(response.status)(equalTo(Status.BadRequest))
						}
				),

				suite("POST /api/v1/todos/create")(
						test("возвращает 200 при успешном создании") {
								val mockService = createMockService(
										createResult = ZIO.unit
								)

								val app = createApp(mockService)
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
													|}""".stripMargin)
								).addHeader(Header.ContentType(MediaType.application.json))

								for {
										response <- app(request)
								} yield assert(response.status)(equalTo(Status.Ok))
						},

						test("возвращает 400 при некорректном JSON") {
								val mockService = createMockService()

								val app = createApp(mockService)
								val request = Request.post(
										decodeUrl(s"api/v1/todos/create"),
										Body.fromString("""{invalid json}""")
								).addHeader(Header.ContentType(MediaType.application.json))

								for {
										response <- app(request)
								} yield assert(response.status)(equalTo(Status.BadRequest))
						},

						test("возвращает 400 при отсутствии Content-Type") {
								val mockService = createMockService()

								val app = createApp(mockService)
								val request = Request.post(
										decodeUrl(s"api/v1/todos/create"),
										Body.fromString("""{"title":"Test"}""")
								)

								for {
										response <- app(request)
								} yield assert(response.status)(equalTo(Status.BadRequest))
						}
				),

				suite("PUT /api/v1/todos/update/{id}")(
						test("возвращает 200 при успешном обновлении") {
								val mockService = createMockService(
										updateResult = ZIO.unit
								)

								val app = createApp(mockService)
								val request = Request.put(
										decodeUrl(s"api/v1/todos/update/${testId.toString}"),
										Body.fromString("""{"title":"Updated","description":"Updated Desc","isDone":true}""")
								).addHeader(Header.ContentType(MediaType.application.json))

								for {
										response <- app(request)
								} yield assert(response.status)(equalTo(Status.Ok))
						},

						test("возвращает 404 при обновлении несуществующего todo") {
								val mockService = createMockService(
										updateResult = ZIO.fail(TodoNotFoundError(testId.toString))
								)

								val app = createApp(mockService)
								val request = Request.put(
										decodeUrl(s"api/v1/todos/update/${testId.toString}"),
										Body.fromString("""{"title":"Updated"}""")
								).addHeader(Header.ContentType(MediaType.application.json))

								for {
										response <- app(request)
								} yield assert(response.status)(equalTo(Status.NotFound))
						}
				),

				suite("DELETE /api/v1/todos/delete/{id}")(
						test("возвращает 200 при успешном удалении") {
								val mockService = createMockService(
										deleteResult = ZIO.unit
								)

								val app = createApp(mockService)
								val request = Request.delete(
										decodeUrl(s"api/v1/todos/delete/${testId.toString}")
								)

								for {
										response <- app(request)
								} yield assert(response.status)(equalTo(Status.Ok))
						}
				)
		)

		// Мок-сервис для тестирования
		private def createMockService(
		                             getResult: Task[Option[TodoItem]] = ZIO.none,
		                             createResult: Task[Unit] = ZIO.unit,
		                             updateResult: Task[Unit] = ZIO.unit,
		                             deleteResult: Task[Unit] = ZIO.unit
		                             ): TodoService = new TodoService {
				override def get(id: UUID): Task[Option[TodoItem]] = getResult

				override def create(request: CreateTodoRequest): Task[Unit] = createResult

				override def update(id: UUID, request: UpdateTodoRequest): Task[Unit] = updateResult

				override def delete(id: UUID): Task[Unit] = deleteResult
		}

		// метод для декодирования из строки в URL
		private def decodeUrl(path: String): URL =
				URL.decode(path).getOrElse {
						throw new IllegalArgumentException(s"Invalid URL path: $path")
				}

		// Создаем HTTP приложение из контроллера
		private def createApp(service: TodoService): Routes[Any, Response] = {
				val controller = new TodoController(service)
				val endpoints = controller.allEndpoints
				ZioHttpInterpreter().toHttp(endpoints)
		}
}
