package todos.controller

import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.tapir.endpoint
import sttp.tapir.ztapir.*
import zio.ZLayer
import zio.metrics.connectors.prometheus.PrometheusPublisher

class MetricsController(publisher: PrometheusPublisher) {
  private val metricsEndpoint: ZServerEndpoint[Any, ZioStreams & WebSockets] =
    endpoint.get
      .in("metrics")
      .out(stringBody)
      .serverLogic(_ => publisher.get.map(Right(_)))

  val allEndpoints = List(metricsEndpoint)
}
object MetricsController {
  val live = ZLayer.fromFunction(new MetricsController(_))
}
