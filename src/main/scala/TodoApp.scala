import io.micrometer.core.instrument.binder.jvm.{
  ClassLoaderMetrics,
  JvmGcMetrics,
  JvmInfoMetrics,
  JvmMemoryMetrics,
  JvmThreadMetrics,
}
import io.micrometer.core.instrument.{Counter, Timer}
import io.micrometer.prometheusmetrics.{PrometheusConfig, PrometheusMeterRegistry}
import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import todos.controller.{AuthController, MetricsController, TodoController}
import todos.service.{AuthServiceImpl, JwtServiceImpl, MigrationService, TodoServiceImpl}
import zio.http.*
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.ZServerEndpoint
import todos.config.{AuthConfig, DataBaseConfig, JwtConfig, ValidationConfig}
import todos.repository.todoimpl.PostgresTodoRepository
import todos.repository.userimpl.PostgresUserRepository
import zio.*
import zio.http.Server

object TodoApp extends ZIOAppDefault {
  type AppEnv = TodoController & AuthController & MigrationService & MetricsController & PrometheusMeterRegistry

  private val prometheusMeterRegistryLayer: ZLayer[Any, Nothing, PrometheusMeterRegistry] = ZLayer.succeed {
    val registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    // Регистрируем JVM-метрики
    new ClassLoaderMetrics().bindTo(registry)
    new JvmMemoryMetrics().bindTo(registry)
    new JvmGcMetrics().bindTo(registry)
    new JvmThreadMetrics().bindTo(registry)
    new JvmInfoMetrics().bindTo(registry)
    registry
  }

  def loggingMiddleware(registry: PrometheusMeterRegistry): Middleware[Any] = {
    val timer = Timer
      .builder("http_request_duration")
      .description("HTTP request duration")
    val counter = Counter
      .builder("http_requests_total")
      .description("Total HTTP requests")

    new Middleware[Any] {
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

            val durationHistogram = Metric
              .histogram(
                "http_request_duration",
                "HTTP request duration in ms",
                Boundaries(Chunk(5.0, 10.0, 25.0, 50.0, 100.0, 250.0, 500.0, 1000.0, 2500.0, 5000.0, 10000.0)),
              )
              .tagged("method", method)
              .tagged("path", path)

            for {
              start <- Clock.nanoTime
              _ <- ZIO.logAnnotate(logAnnotations) {
                ZIO.logInfo(s"$method $path")
              }
              response <- h(request)
              end <- Clock.nanoTime
              duration = (end - start) / 1_000_000.0
              _ <- Metric
                .counter("http_requests_total", "Total HTTP requests")
                .tagged("method", method)
                .tagged("path", path)
                .tagged("code", response.status.code.toString)
                .increment
              _ <- durationHistogram.update(duration)
              _ <- ZIO.logAnnotate(logAnnotations) {
                ZIO.logInfo(s"${duration}ms with status ${response.status.code}")
              }
            } yield response
          }
        }
      }
  }

  val appLayer: ZLayer[Any, Throwable, AppEnv] = ZLayer.make[AppEnv](
    // prometheus
    prometheusMeterRegistryLayer,
    // configs
    ValidationConfig.live,
    DataBaseConfig.live,
    DataBaseConfig.configLive,
    JwtConfig.live,
    AuthConfig.live,
    // repositories
    PostgresTodoRepository.live,
    PostgresUserRepository.live,
    // services
    AuthServiceImpl.live,
    JwtServiceImpl.live,
    TodoServiceImpl.live,
    MigrationService.live,
    // controllers
    AuthController.live,
    TodoController.live,
    MetricsController.live,
  )

  override def run: ZIO[Any, Throwable, Unit] = (for {
    port <- System.env("HTTP_PORT").map(_.flatMap(_.toIntOption).getOrElse(8080))
    // controllers
    todoController <- ZIO.service[TodoController]
    authController <- ZIO.service[AuthController]
    metrics <- ZIO.service[MetricsController]

    // metrics
    registry <- ZIO.service[PrometheusMeterRegistry]

    migration <- ZIO.service[MigrationService]

    _ <- ZIO.logInfo(s"Starting server on port $port")
    _ <- migration.migrate

    apiEndpoints: List[ZServerEndpoint[Any, ZioStreams & WebSockets]] =
      todoController.allEndpoints ++ authController.allEndpoints

    swaggerEndpoints: List[ZServerEndpoint[Any, ZioStreams & WebSockets]] = SwaggerInterpreter()
      .fromServerEndpoints(apiEndpoints, "Todo API", "1.0")
    allEndpoints = apiEndpoints ++ swaggerEndpoints ++ metrics.allEndpoints

    baseApp: Routes[Any, Response] = ZioHttpInterpreter().toHttp(allEndpoints)

    finalApp = baseApp @@ loggingMiddleware(registry)

    _ <- Server
      .serve(finalApp)
      .provide(
        ZLayer.succeed(Server.Config.default.port(port)),
        Server.live,
      )
  } yield ()).provideLayer(appLayer)
}
