package todos.controller

import sttp.tapir.endpoint
import sttp.tapir.*
import sttp.tapir.stringBody
import sttp.tapir.ztapir.*
import todos.service.JobsService
import zio.*

//import java.time.Instant

class JobsController(jobsService: JobsService) {

  private val baseEndpoint = endpoint
    .tag("jobs")
    .in("api" / "v1" / "migrate")


  val migrateEndpoint: ZServerEndpoint[Any, Any] = baseEndpoint.post
    .out(stringBody)
    .description("Apply database migrations")
    .zServerLogic { _ =>
      ZIO.succeed("Hello world")
    }

  val allEndpoints: List[ZServerEndpoint[Any, Any]] = List(migrateEndpoint)
}

object JobsController {
  def make(jobsService: JobsService): JobsController =
    new JobsController(jobsService)
}
