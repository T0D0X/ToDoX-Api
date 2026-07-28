package todos.middleware

import todos.config.CorsAllowedConfig

import zio.{ZIO, ZLayer}

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

object Middleware {
  val live = ZLayer.fromZIO {
    for {
      registry <- ZIO.service[PrometheusMeterRegistry]
      config <- ZIO.service[CorsAllowedConfig]
    } yield LoggingMiddleware(registry) ++ CorsMiddleware(config)
  }
}
