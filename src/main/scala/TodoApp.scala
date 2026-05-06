import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import todos.controller.{AuthController, TodoController}
import todos.service.{AuthServiceImpl, JwtServiceImpl, TodoServiceImpl}
import zio.http.*
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.ZServerEndpoint
import todos.config.{AuthConfig, DataBaseConfig, JwtConfig}
import todos.repository.todoimpl.PostgresTodoRepository
import todos.repository.userimpl.PostgresUserRepository
import zio.*
import zio.http.Server

object TodoApp extends ZIOAppDefault {
  type AppEnv = TodoController & AuthController

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

  val appLayer: ZLayer[Any, Throwable, AppEnv] = ZLayer.make[AppEnv](
    // configs
    DataBaseConfig.live,
    JwtConfig.live,
    AuthConfig.live,
    // repositories
    PostgresTodoRepository.live,
    PostgresUserRepository.live,
    // services
    AuthServiceImpl.live,
    JwtServiceImpl.live,
    TodoServiceImpl.live,
    // controllers
    AuthController.live,
    TodoController.live,
  )

  override def run: ZIO[Any, Throwable, Unit] = (for {
    port <- System.env("PORT").map(_.flatMap(_.toIntOption).getOrElse(9090))
    todoController <- ZIO.service[TodoController]
    authController <- ZIO.service[AuthController]

    _ <- ZIO.logInfo(s"Starting server on port $port")

    apiEndpoints: List[ZServerEndpoint[Any, ZioStreams & WebSockets]] =
      (todoController.allEndpoints ++ authController.allEndpoints)
        .asInstanceOf[List[ZServerEndpoint[Any, ZioStreams & WebSockets]]]

    swaggerEndpoints: List[ZServerEndpoint[Any, ZioStreams & WebSockets]] = SwaggerInterpreter()
      .fromServerEndpoints(apiEndpoints, "Todo API", "1.0")
    allEndpoints = apiEndpoints ++ swaggerEndpoints

    baseApp: Routes[Any, Response] = ZioHttpInterpreter().toHttp(allEndpoints)

    finalApp = baseApp @@ loggingMiddleware

    _ <- Server
      .serve(finalApp)
      .provide(
        ZLayer.succeed(Server.Config.default.port(port)),
        Server.live,
      )
  } yield ()).provideLayer(appLayer)
}
