import doobie.*
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import todos.controller.TodoController
import todos.repository.PostgresTodoRepository
import todos.service.TodoServiceImpl
import zio.http.*
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import zio.interop.catz.asyncInstance
import zio.*
import zio.http.Server

import java.util.Properties

object TodoApp extends ZIOAppDefault {

		private val transactorLayer: ZLayer[Any, Throwable, Transactor[Task]] =
				ZLayer.scoped {
						ZIO.acquireRelease(createTransactor)(_ => ZIO.unit)
				}

		override def run: ZIO[Any, Throwable, Unit] = {
				val program = for {
						port <- System.env("PORT").map(_.flatMap(_.toIntOption).getOrElse(9090))
						xa <- createTransactor
						repository = PostgresTodoRepository(xa)
						service = TodoServiceImpl.make(repository)
						controller = new TodoController(service)

						_ <- ZIO.logInfo(s"Starting server on port $port")

						apiEndpoints = controller.allEndpoints
						swaggerEndpoints = SwaggerInterpreter()
						.fromServerEndpoints[Task](
								apiEndpoints,
								"Todo API",
								"1.0"
						)
						allEndpoints = apiEndpoints ++ swaggerEndpoints

						app: Routes[Any, Response] = ZioHttpInterpreter().toHttp(allEndpoints)

						_ <- Server.serve(app).provide(
								ZLayer.succeed(Server.Config.default.port(port)),
								Server.live
						)
				} yield ()

				program.provide(transactorLayer)
		}

		private def createTransactor: ZIO[Any, Throwable, Transactor[Task]] = {
				for {
						dbUser <- System.env("DB_USER").map(_.getOrElse("test_user"))
						dbPassword <- System.env("DB_PASSWORD").map(_.getOrElse("test_password"))
						dbHost <- System.env("DB_HOST").map(_.getOrElse("localhost"))
						dbName <- System.env("DB_NAME").map(_.getOrElse("todo_test"))
						dbPort <- System.env("DB_PORT").map(_.getOrElse("8080"))

						_ <- ZIO.logInfo(s"DB_HOST:$dbHost")
						_ <- ZIO.logInfo(s"DB_NAME:$dbName")
						_ <- ZIO.logInfo(s"DB_PORT:$dbPort")
						jdbcUrl = s"jdbc:postgresql://$dbHost:$dbPort/$dbName"

						_ <- ZIO.logInfo(s"Connecting to database:  $jdbcUrl")

						transactor <- ZIO.attempt {
								val props = new Properties()
								props.setProperty("user", dbUser)
								props.setProperty("password", dbPassword)
								Transactor.fromDriverManager[Task](
										driver = "org.postgresql.Driver",
										url = jdbcUrl,
										props,
										logHandler = None
								)
						}
				} yield transactor
		}.tapError { error =>
				ZIO.logError(s"Failed to create database transactor: ${error.getMessage}")
		}.tap { transactor =>
				ZIO.logInfo("Database transactor created successfully")
		}
}