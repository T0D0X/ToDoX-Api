import todos.config.*
import todos.controller.*
import todos.middleware.Middleware as CommonMiddleware
import todos.models.UserData
import todos.redis.{RedisCache, RedisConnection}
import todos.repository.todoimpl.PostgresTodoRepository
import todos.repository.userimpl.PostgresUserRepository
import todos.service.*

import sttp.capabilities.zio.ZioStreams
import sttp.capabilities.WebSockets
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.ZServerEndpoint
import zio.*
import zio.http.*

import io.micrometer.core.instrument.binder.jvm.{
  ClassLoaderMetrics,
  JvmGcMetrics,
  JvmInfoMetrics,
  JvmMemoryMetrics,
  JvmThreadMetrics,
}
import io.micrometer.prometheusmetrics.{PrometheusConfig, PrometheusMeterRegistry}

object TodoApp extends ZIOAppDefault {
  type AppEnv =
    TodoController & AuthController & MigrationService & MetricsController & PrometheusMeterRegistry & Middleware[Any]

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

  val appLayer: ZLayer[Any, Throwable, AppEnv] = ZLayer.make[AppEnv](
    // prometheus
    prometheusMeterRegistryLayer,
    // configs
    ValidationConfig.live,
    DataBaseConfig.live,
    DataBaseConfig.configLive,
    JwtConfig.live,
    AuthConfig.live,
    RedisConfig.live,
    CorsAllowedConfig.live,
    // cache
    RedisConnection.live,
    RedisCache.live[String, UserData]("users"),
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
    // middleware
    CommonMiddleware.live,
  )

  override def run: ZIO[Any, Throwable, Unit] = (for {
    port <- System.env("HTTP_PORT").map(_.flatMap(_.toIntOption).getOrElse(8080))
    // controllers
    todoController <- ZIO.service[TodoController]
    authController <- ZIO.service[AuthController]
    metrics <- ZIO.service[MetricsController]

    // metrics
    registry <- ZIO.service[PrometheusMeterRegistry]

    middleware <- ZIO.service[Middleware[Any]]

    migration <- ZIO.service[MigrationService]

    _ <- ZIO.logInfo(s"Starting server on port $port")
    _ <- migration.migrate

    apiEndpoints: List[ZServerEndpoint[Any, ZioStreams & WebSockets]] =
      todoController.allEndpoints ++ authController.allEndpoints

    swaggerEndpoints: List[ZServerEndpoint[Any, ZioStreams & WebSockets]] = SwaggerInterpreter()
      .fromServerEndpoints(apiEndpoints, "Todo API", "1.0")
    allEndpoints = apiEndpoints ++ swaggerEndpoints ++ metrics.allEndpoints

    baseApp: Routes[Any, Response] = ZioHttpInterpreter().toHttp(allEndpoints)

    finalApp = baseApp @@ middleware

    _ <- Server
      .serve(finalApp)
      .provide(
        ZLayer.succeed(Server.Config.default.port(port)),
        Server.live,
      )
  } yield ()).provideLayer(appLayer)
}
