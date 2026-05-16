package todos.controller

import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.tapir.{endpoint, ValidationError, ValidationResult, Validator}
import sttp.tapir.generic.auto.*
import todos.service.AuthService
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.ztapir.*
import todos.config.{AuthConfig, ValidationConfig}
import todos.errors.ErrorResponse
import todos.util.EndpointSupport.{standardErrorOut, toErrorResponse}
import todos.models.{CreateUserRequest, JwtResponse, LoginRequest, UserResponse}
import zio.{ZIO, ZLayer}

class AuthController(
    authService: AuthService,
    authConfig: AuthConfig,
    validationConfig: ValidationConfig,
) {

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
    .in(
      jsonBody[CreateUserRequest].validate(Validator.custom { req =>
        val errors = List(
          validationConfig.validateEmail(req.email),
          validationConfig.validatePhone(req.phone),
          validationConfig.validatePassword(req.password),
        ).collect { case ValidationResult.Invalid(msgs) => msgs }.flatten
        if (errors.isEmpty) ValidationResult.Valid
        else ValidationResult.Invalid(errors)
      }),
    )
    .mapErrorOut { (error: ErrorResponse | List[ValidationError[?]]) =>
      error match {
        case err: ErrorResponse => err
        case validationErrors: List[ValidationError[?]] =>
          val messages = validationErrors.flatMap(_.customMessage).mkString(", ")
          ErrorResponse("Validation failed", "VALIDATION", messages)
      }
    }(error => error)
    .out(jsonBody[UserResponse])
    .description("Create User")
    .serverLogic { _ => request =>
      authService.register(request).mapError(toErrorResponse)
    }

  // DELETE api/v1/users/delete
  val deleteEndPoint = baseEndpoint.delete
    .in("delete")
    .in(jsonBody[LoginRequest].validate(Validator.custom(req => validationConfig.validatePassword(req.password))))
    .description("Delete user")
    .serverLogic { _ => request =>
      authService.delete(request).mapError(toErrorResponse)
    }

  // POST api/v1/users/login
  val loginEndpoint = baseEndpoint.post
    .in("login")
    .in(jsonBody[LoginRequest].validate(Validator.custom(req => validationConfig.validatePassword(req.password))))
    .out(jsonBody[JwtResponse])
    .description("Login User")
    .serverLogic { _ => request =>
      authService.login(request).mapError(toErrorResponse)
    }

  val allEndpoints = List(createEndpoint, deleteEndPoint, loginEndpoint)
    .asInstanceOf[List[ZServerEndpoint[Any, ZioStreams & WebSockets]]]
}

object AuthController {
  val live = ZLayer.fromFunction(new AuthController(_, _, _))
}
