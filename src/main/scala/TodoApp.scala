import doobie.*
import org.http4s.HttpApp
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import todos.controller.TodoController
import todos.repository.{PostgresTodoRepository, TodoRepository}
import todos.service.TodoServiceImpl
import zio.interop.catz.asyncInstance
import cats.effect.kernel.Async
import zio.{Scope, Task, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}
import zio.http.Server

import java.util.Properties

object TodoApp extends zio.ZIOApp {

		val serverConfig = ZLayer.succeed(Server.Config.default.port(8080))

		override val bootstrap: ZLayer[ZIOAppArgs, Any, Server] =
				serverConfig >>> Server.live

		override def run: ZIO[Server with ZIOAppArgs with Scope, Any, Any] = {
				for {
						xa <- createTransactor
						repository = PostgresTodoRepository(xa)
						service = TodoServiceImpl.make(repository)
						controller = new TodoController(service)
						httpApp = ZioHttpInterpreter().toHttp(controller.allEndpoints)
						_ <- Server.serve(httpApp)
				} yield ()
		}

		private def createTransactor =
				ZIO.attempt {
						val props = new Properties()
						props.setProperty("user", "test_user")
						props.setProperty("password", "test_password")

						import zio.interop.catz.asyncInstance

						Transactor.fromDriverManager[Task](
								driver = "org.postgresql.Driver",
								url = "jdbc:postgresql://localhost:5432/todo_test",
								props,
								logHandler = None
						)
				}
}
