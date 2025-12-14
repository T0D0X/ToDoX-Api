import doobie.*
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import todos.controller.TodoController
import todos.repository.PostgresTodoRepository
import todos.service.TodoServiceImpl
import zio.http.*
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import zio.*
import zio.http.Server

import java.util.Properties

object TodoApp extends ZIOAppDefault {


		private val transactorLayer: ZLayer[Any, Throwable, Transactor[Task]] =
				ZLayer.scoped {
						ZIO.acquireRelease(
								createTransactor
						)(_ => ZIO.unit)
				}

		override def run: ZIO[Any, Throwable, Unit] = {
				val program = for {
						xa <- createTransactor
						repository = PostgresTodoRepository(xa)
						service = TodoServiceImpl.make(repository)
						controller = new TodoController(service)

						apiEndpoints = controller.allEndpoints

						swaggerEndpoints = SwaggerInterpreter()
						.fromServerEndpoints[Task](
								apiEndpoints,
								"Todo API",
								"1.0"
						)
						allEndpoints = apiEndpoints ++ swaggerEndpoints

						app: Routes[Any, Response] = ZioHttpInterpreter().toHttp(allEndpoints)

						_ <- Server.serve(app)
				} yield ()

				program.provide(
						transactorLayer,
						ZLayer.succeed(Server.Config.default.port(9090)),
						Server.live,
				)
		}

		private def createTransactor =
				ZIO.attempt {
						val props = new Properties()
						props.setProperty("user", "test_user")
						props.setProperty("password", "test_password")

						import zio.interop.catz.asyncInstance

						Transactor.fromDriverManager[Task](
								driver = "org.postgresql.Driver",
								url = "jdbc:postgresql://localhost:8080/todo_test",
								props,
								logHandler = None
						)
				}.tapError { error =>
						ZIO.logError(s"Failed to create database transactor: ${error.getMessage}")
				}.tap { transactor =>
						ZIO.logInfo("Database transactor created successfully")
				}
}
