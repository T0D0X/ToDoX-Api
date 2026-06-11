package todos.controller

import sttp.capabilities.zio.ZioStreams
import sttp.capabilities.WebSockets
import sttp.tapir.endpoint
import sttp.tapir.ztapir.*
import zio.{ZIO, ZLayer}

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

class MetricsController(registry: PrometheusMeterRegistry) {
  private val metricsEndpoint: ZServerEndpoint[Any, ZioStreams & WebSockets] =
    endpoint.get
      .in("metrics")
      .out(stringBody)
      .serverLogic(_ => ZIO.succeed(registry.scrape()).map(Right(_)))

  val allEndpoints = List(metricsEndpoint)
}
object MetricsController {
  val live = ZLayer.fromFunction(new MetricsController(_))
}
