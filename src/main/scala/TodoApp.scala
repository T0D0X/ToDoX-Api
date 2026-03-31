import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import todos.controller.TodoController
import todos.service.TodoServiceImpl
import zio.http.*
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import todos.config.DataBaseConfig
import todos.repository.todoimpl.PostgresTodoRepository
import zio.*
import zio.http.Server

object TodoApp extends ZIOAppDefault {

  val loggingMiddleware: Middleware[Any] = new Middleware[Any] {
    def apply[Env1 <: Any, Err](routes: Routes[Env1, Err]): Routes[Env1, Err] =
      routes.transform[Env1] { h =>
        Handler.scoped[Env1] {
          handler { (request: Request) =>
            val method = request.method.toString()
            val path = request.url.path.toString

            val logAnnotations = Set(
              LogAnnotation("method", method),
              LogAnnotation("path", path),
            )

            for {
              start <- Clock.nanoTime
              _ <- ZIO.logAnnotate(logAnnotations) {
                ZIO.logInfo(s"$method $path")
              }
              response <- h(request)
              end <- Clock.nanoTime
              duration = (end - start) / 1_000_000.0
              _ <- ZIO.logAnnotate(logAnnotations) {
                ZIO.logInfo(s"${duration}ms with status ${response.status.code}")
              }
            } yield response
          }
        }
      }
  }

  val appLayer =
    DataBaseConfig.todoLayer >>>
      ZLayer.fromFunction(PostgresTodoRepository(_)) >>>
      ZLayer.fromFunction(TodoServiceImpl.make) >>>
      ZLayer.fromFunction(new TodoController(_))

  override def run: ZIO[Any, Throwable, Unit] = (for {
    port <- System.env("PORT").map(_.flatMap(_.toIntOption).getOrElse(9090))
    controller <- ZIO.service[TodoController]

    _ <- ZIO.logInfo(s"Starting server on port $port")

    apiEndpoints = controller.allEndpoints
    swaggerEndpoints = SwaggerInterpreter()
      .fromServerEndpoints[Task](
        apiEndpoints,
        "Todo API",
        "1.0",
      )
    allEndpoints = apiEndpoints ++ swaggerEndpoints

    baseApp: Routes[Any, Response] = ZioHttpInterpreter().toHttp(allEndpoints)

    finalApp = baseApp @@ loggingMiddleware

    _ <- Server
      .serve(finalApp)
      .provide(
        ZLayer.succeed(Server.Config.default.port(port)),
        Server.live,
      )
  } yield ()).provideSomeLayer[Any](appLayer)
}
