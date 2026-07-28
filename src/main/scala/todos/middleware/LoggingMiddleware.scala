package todos.middleware

import zio.{Clock, LogAnnotation, ZIO}
import zio.http.{handler, Handler, Middleware, Path, Request, Routes}

import io.micrometer.core.instrument.{Counter, Timer}
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

object LoggingMiddleware {
  private val timer = Timer
    .builder("http_request_duration")
    .description("HTTP request duration")
  private val counter = Counter
    .builder("http_requests_total")
    .description("Total HTTP requests")
  private def normalizePath(path: Path): String = {
    val segments = path.segments.filter(_.nonEmpty)
    val normalized = segments.map {
      case seg if seg.matches("\\d+") => "{id}"
      case seg if seg.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}") => "{id}"
      case other => other
    }
    "/" + normalized.mkString("/")
  }

  def apply(registry: PrometheusMeterRegistry) = new Middleware[Any] {
    def apply[Env1 <: Any, Err](routes: Routes[Env1, Err]): Routes[Env1, Err] =
      routes.transform[Env1] { h =>
        Handler.scoped[Env1] {
          handler { (request: Request) =>
            val method = request.method.toString()
            val path = normalizePath(request.url.path)

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
              _ <- ZIO.succeed {
                counter
                  .tag("method", method)
                  .tag("path", path)
                  .tag("code", response.status.code.toString)
                  .register(registry)
                  .increment()

                timer
                  .tag("method", method)
                  .tag("path", path)
                  .register(registry)
                  .record(duration.toLong, java.util.concurrent.TimeUnit.MILLISECONDS)
              }
              _ <- ZIO.logAnnotate(logAnnotations) {
                ZIO.logInfo(s"${duration}ms with status ${response.status.code}")
              }
            } yield response
          }
        }
      }
  }
}
