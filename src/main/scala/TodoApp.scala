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
import zio.metrics.Metric
import zio.metrics.MetricKeyType.Histogram.Boundaries
import zio.metrics.jvm.{DefaultJvmMetrics, GarbageCollector, MemoryAllocation, MemoryPools, Thread, VersionInfo}
import zio.metrics.connectors.MetricsConfig
import zio.metrics.connectors.prometheus.{prometheusLayer, PrometheusPublisher}

object TodoApp extends ZIOAppDefault {
  type AppEnv = TodoController & AuthController & MigrationService & MetricsController & GarbageCollector &
    MemoryAllocation & MemoryPools & Thread & VersionInfo

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
    // infra
    ZLayer.succeed(MetricsConfig(10.seconds)),
    ZLayer.fromZIO(PrometheusPublisher.make),
    DefaultJvmMetrics.liveV2,
    prometheusLayer,
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
    _ <- ZIO.service[GarbageCollector] <&>
      ZIO.service[MemoryAllocation] <&>
      ZIO.service[MemoryPools] <&>
      ZIO.service[Thread] <&>
      ZIO.service[VersionInfo]

    migration <- ZIO.service[MigrationService]

    _ <- ZIO.logInfo(s"Starting server on port $port")
    _ <- migration.migrate

    apiEndpoints: List[ZServerEndpoint[Any, ZioStreams & WebSockets]] =
      todoController.allEndpoints ++ authController.allEndpoints

    swaggerEndpoints: List[ZServerEndpoint[Any, ZioStreams & WebSockets]] = SwaggerInterpreter()
      .fromServerEndpoints(apiEndpoints, "Todo API", "1.0")
    allEndpoints = apiEndpoints ++ swaggerEndpoints ++ metrics.allEndpoints

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
