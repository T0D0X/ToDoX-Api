package todos.controller

import sttp.tapir.endpoint
import sttp.tapir.generic.auto.*
import todos.service.AuthService
import sttp.model.StatusCode
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir.*
import todos.config.AuthConfig
import todos.errors.{AppError, ErrorResponse}
import todos.models.{CreateUserRequest, UserResponse}
import zio.{ZIO, ZLayer}

import java.time.Instant

class AuthController(authService: AuthService, authConfig: AuthConfig) {

  private val notFoundOutput =
    jsonBody[ErrorResponse]
      .description("Пользователь не найден")
      .example(
        ErrorResponse(
          error = "UserNotFoundError",
          code = "NOT_FOUND",
          message = "User with id 123e4567-e89b-12d3-a456-426614174000 not found",
          timestamp = Instant.parse("2021-10-01T12:00:00Z"),
        ),
      )

  private def toErrorResponse(throwable: Throwable): ErrorResponse = throwable match {
    case error: AppError => ErrorResponse.fromAppError(error)
    case ex: Throwable =>
      ErrorResponse(
        error = "DatabaseError",
        code = "DB",
        message = ex.getMessage,
      )
  }

  private val createUserEndpoint: ZServerEndpoint[Any, Any] = endpoint
    .tag("auth")
    .in("api" / "v1" / "users")
    .in("register")
    .in(jsonBody[CreateUserRequest])
    .securityIn(auth.bearer[String]())
    .out(jsonBody[UserResponse])
    .errorOut(
      oneOf[ErrorResponse](
        oneOfVariantValueMatcher(StatusCode.NotFound, notFoundOutput) {
          case ErrorResponse(_, code, _, _) if code.startsWith("NOT_FOUND") => true
        },
      ),
    )
    .zServerSecurityLogic { token =>
      if (token == authConfig.authToken) ZIO.succeed(())
      else ZIO.fail(ErrorResponse("Unauthorized", "AUTH", "Invalid admin token", Instant.now()))
    }
    .description("Create User")
    .serverLogic { _ => request =>
      authService.register(request).mapError(toErrorResponse)
    }

  val allEndpoints = List(createUserEndpoint)
}

object AuthController {
  val live = ZLayer.fromFunction(new AuthController(_, _))
}
