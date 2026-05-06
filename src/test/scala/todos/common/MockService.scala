package todos.common

import todos.errors.AppErrors.*

import java.util.UUID
import todos.models.{
  CreateTodoRequest,
  CreateUserRequest,
  JwtResponse,
  LoginRequest,
  TodoItem,
  UpdateTodoRequest,
  UserResponse,
}
import todos.service.{AuthService, JwtService, TodoService}
import zio.{IO, Task, ZIO}

object MockService {
  val tokenTest = "token-test"

  def mockTodoService(
      getResult: Task[Option[TodoItem]] = ZIO.none,
      createResult: Task[Unit] = ZIO.unit,
      updateResult: Task[Unit] = ZIO.unit,
      deleteResult: Task[Unit] = ZIO.unit,
      getByUserIdResult: Task[List[TodoItem]] = ZIO.succeed(List.empty[TodoItem]),
  ): TodoService = new TodoService {
    override def get(id: UUID): Task[Option[TodoItem]] = getResult
    override def create(request: CreateTodoRequest): Task[Unit] = createResult
    override def update(id: UUID, request: UpdateTodoRequest): Task[Unit] = updateResult
    override def delete(id: UUID): Task[Unit] = deleteResult
    override def getByUserId(userId: UUID): Task[List[TodoItem]] = getByUserIdResult
  }

  def mockJwtService(
      uuidReturn: Option[UUID] = None,
  ): JwtService = new JwtService {
    override def generateToken(userId: UUID): Task[String] = ZIO.succeed(tokenTest)
    override def validateToken(token: String): IO[AuthErrorBase, UUID] =
      ZIO.ifZIO(ZIO.succeed(token == tokenTest && uuidReturn.isDefined))(
        onTrue = ZIO.succeed(uuidReturn.get),
        onFalse = ZIO.fail(InvalidTokenError(token)),
      )
  }

  def mockAuthService(userResponse: UserResponse): AuthService = new AuthService {
    override def register(request: CreateUserRequest): Task[UserResponse] =
      ZIO.cond(
        request.login != "incorrect",
        userResponse,
        UserAlreadyExistsError(request.login),
      )

    override def login(request: LoginRequest): Task[JwtResponse] =
      request match {
        case LoginRequest(login, _) if login == "incorrect" => ZIO.fail(UserNotFoundError(login))
        case LoginRequest(_, password) if password == "invalid" => ZIO.fail(PasswordError(password))
        case _ => ZIO.succeed(JwtResponse(tokenTest, userResponse))
      }
  }
}
