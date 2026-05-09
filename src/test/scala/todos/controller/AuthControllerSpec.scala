package todos.controller

import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import todos.config.AuthConfig
import todos.service.AuthService
import todos.common.{MockService, TestRequests}
import todos.errors.ErrorResponse
import todos.models.{CreateUserRequest, JwtResponse, LoginRequest, UserResponse}
import zio.http.{Response, Routes, Status}
import zio.test.*
import zio.json.*

import java.util.UUID

object AuthControllerSpec extends ZIOSpecDefault {
  private val userResponse = UserResponse(
    userId = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
    login = "login",
    email = "string@gmail.com",
    phone = "+799999999",
  )
  private val createUserRequest = CreateUserRequest(
    login = userResponse.login,
    email = userResponse.email,
    phone = userResponse.phone,
    password = "password",
  )
  private val loginRequest = LoginRequest(
    createUserRequest.login,
    createUserRequest.password,
  )
  val token = "token"

  override def spec =
    suite("AuthControllerSpec")(
      suite("POST api/v1/users/register")(
        test("return 200") {
          val app = makeApp(MockService.mockAuthService(userResponse))
          val request = TestRequests.post("api/v1/users/register", createUserRequest.toJson, token)
          for {
            response <- app(request)
            body <- response.body.asJson[UserResponse]
          } yield assertTrue(
            response.status == Status.Ok,
            body == userResponse,
          )
        },
        test("return 401 because login already exists") {
          val app = makeApp(MockService.mockAuthService(userResponse))
          val request =
            TestRequests.post("api/v1/users/register", createUserRequest.copy(login = "incorrect").toJson, token)
          for {
            response <- app(request)
            body <- response.body.asJson[ErrorResponse]
          } yield assertTrue(
            response.status == Status.Unauthorized,
            body.error == "AuthError",
            body.code == "AUTH_ERROR_1",
            body.message == "User with incorrect already exists",
          )
        },
        test("return 401 because invalid token") {
          val app = makeApp(MockService.mockAuthService(userResponse))
          val request = TestRequests.post("api/v1/users/register", createUserRequest.toJson, "invalid")
          for {
            response <- app(request)
            body <- response.body.asJson[ErrorResponse]
          } yield assertTrue(
            response.status == Status.Unauthorized,
            body.error == "Forbidden",
            body.code == "AUTH",
            body.message == "Invalid admin token",
          )
        },
      ),
      suite("POST api/v1/users/login")(
        test("return 200") {
          val app = makeApp(MockService.mockAuthService(userResponse))
          val request = TestRequests.post("api/v1/users/login", loginRequest.toJson, token)
          for {
            response <- app(request)
            body <- response.body.asJson[JwtResponse]
          } yield assertTrue(
            response.status == Status.Ok,
            body.token == MockService.tokenTest,
            body.user == userResponse,
          )
        },
        test("return 401 because User not found") {
          val app = makeApp(MockService.mockAuthService(userResponse))
          val request = TestRequests.post("api/v1/users/login", loginRequest.copy(login = "incorrect").toJson, token)
          for {
            response <- app(request)
            body <- response.body.asJson[ErrorResponse]
          } yield assertTrue(
            response.status == Status.Unauthorized,
            body.error == "AuthError",
            body.code == "AUTH_ERROR_2",
            body.message == "User with incorrect not found",
          )
        },
        test("return 401 because password invalid") {
          val app = makeApp(MockService.mockAuthService(userResponse))
          val request = TestRequests.post("api/v1/users/login", loginRequest.copy(password = "invalid").toJson, token)
          for {
            response <- app(request)
            body <- response.body.asJson[ErrorResponse]
          } yield assertTrue(
            response.status == Status.Unauthorized,
            body.error == "AuthError",
            body.code == "AUTH_ERROR_3",
            body.message == "invalid incorrect",
          )
        },
        test("return 401 because invalid token") {
          val app = makeApp(MockService.mockAuthService(userResponse))
          val request = TestRequests.post("api/v1/users/login", loginRequest.toJson, "invalid")
          for {
            response <- app(request)
            body <- response.body.asJson[ErrorResponse]
          } yield assertTrue(
            response.status == Status.Unauthorized,
            body.error == "Forbidden",
            body.code == "AUTH",
            body.message == "Invalid admin token",
          )
        },
      ),
      suite("DELETE api/v1/users/delete")(
        test("return 200") {
          val app = makeApp(MockService.mockAuthService(userResponse))
          val request = TestRequests.delete("api/v1/users/delete", token, Some(loginRequest.toJson))
          for {
            response <- app(request)
          } yield assertTrue(
            response.status == Status.Ok,
            response.body.isEmpty,
          )
        },
        test("return 401 because User not found") {
          val app = makeApp(MockService.mockAuthService(userResponse))
          val request =
            TestRequests.delete("api/v1/users/delete", token, Some(loginRequest.copy(login = "incorrect").toJson))
          for {
            response <- app(request)
            body <- response.body.asJson[ErrorResponse]
          } yield assertTrue(
            response.status == Status.Unauthorized,
            body.error == "AuthError",
            body.code == "AUTH_ERROR_2",
            body.message == "User with incorrect not found",
          )
        },
        test("return 401 because password invalid") {
          val app = makeApp(MockService.mockAuthService(userResponse))
          val request =
            TestRequests.delete("api/v1/users/delete", token, Some(loginRequest.copy(password = "invalid").toJson))
          for {
            response <- app(request)
            body <- response.body.asJson[ErrorResponse]
          } yield assertTrue(
            response.status == Status.Unauthorized,
            body.error == "AuthError",
            body.code == "AUTH_ERROR_3",
            body.message == "invalid incorrect",
          )
        },
        test("return 401 because invalid token") {
          val app = makeApp(MockService.mockAuthService(userResponse))
          val request = TestRequests.delete("api/v1/users/delete", "invalid", Some(loginRequest.toJson))
          for {
            response <- app(request)
            body <- response.body.asJson[ErrorResponse]
          } yield assertTrue(
            response.status == Status.Unauthorized,
            body.error == "Forbidden",
            body.code == "AUTH",
            body.message == "Invalid admin token",
          )
        },
      ),
    )

  private def makeApp(
      authService: AuthService,
      authConfig: AuthConfig = AuthConfig("token"),
  ): Routes[Any, Response] = {
    val controller = new AuthController(authService, authConfig)
    ZioHttpInterpreter().toHttp(controller.allEndpoints)
  }
}
