package todos.controller

import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.tapir.endpoint
import sttp.tapir.generic.auto.*
import todos.service.AuthService
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.ztapir.*
import todos.config.AuthConfig
import todos.errors.ErrorResponse
import todos.util.EndpointSupport.{standardErrorOut, toErrorResponse}
import todos.models.{CreateUserRequest, JwtResponse, LoginRequest, UserResponse}
import zio.{ZIO, ZLayer}

class AuthController(authService: AuthService, authConfig: AuthConfig) {

  private val baseEndpoint = endpoint
    .tag("auth")
    .in("api" / "v1" / "users")
    .securityIn(auth.bearer[Option[String]]())
    .errorOut(standardErrorOut)
    .zServerSecurityLogic {
      case Some(token) if token == authConfig.authToken => ZIO.succeed(())
      case _ => ZIO.fail(ErrorResponse("Forbidden", "AUTH", "Invalid admin token"))
    }

  // POST api/v1/users/register
  val createEndpoint = baseEndpoint.post
    .in("register")
    .in(jsonBody[CreateUserRequest])
    .out(jsonBody[UserResponse])
    .description("Create User")
    .serverLogic { _ => request =>
      authService.register(request).mapError(toErrorResponse)
    }

  // POST api/v1/users/login
  val loginEndpoint = baseEndpoint.post
    .in("login")
    .in(jsonBody[LoginRequest])
    .out(jsonBody[JwtResponse])
    .description("Login User")
    .serverLogic { _ => request =>
      authService.login(request).mapError(toErrorResponse)
    }

  val allEndpoints = List(createEndpoint, loginEndpoint)
    .asInstanceOf[List[ZServerEndpoint[Any, ZioStreams & WebSockets]]]
}

object AuthController {
  val live = ZLayer.fromFunction(new AuthController(_, _))
}
