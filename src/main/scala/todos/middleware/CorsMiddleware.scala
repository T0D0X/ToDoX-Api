package todos.middleware

import todos.config.CorsAllowedConfig

import zio.http.Header.*
import zio.http.Middleware
import zio.http.Middleware.CorsConfig

object CorsMiddleware {
  private def make(config: CorsAllowedConfig) =
    CorsConfig(
      allowedOrigin = { origin =>
        val originValue = origin.toString
        Option.when(config.origins.contains(originValue))(AccessControlAllowOrigin.Specific(origin))
      },
      allowedMethods = AccessControlAllowMethods(config.methods.toSeq: _*),
      allowedHeaders = AccessControlAllowHeaders(config.headers.toSeq: _*),
      allowCredentials =
        if (config.credentials) AccessControlAllowCredentials.Allow else AccessControlAllowCredentials.DoNotAllow,
    )

  def apply(cfg: CorsAllowedConfig): Middleware[Any] = Middleware.cors(make(cfg))
}
