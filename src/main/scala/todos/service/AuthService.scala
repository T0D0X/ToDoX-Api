package todos.service

import todos.errors.AppErrors.{PasswordError, UserAlreadyExistsError, UserNotFoundError}
import todos.models.{CreateUserRequest, JwtResponse, LoginRequest, UserData, UserResponse}
import todos.repository.userimpl.UserRepository
import todos.util.HashingUtil
import zio.{Task, ZIO, ZLayer}

import java.util.UUID

trait AuthService {
  def register(request: CreateUserRequest): Task[UserResponse]
  def delete(request: LoginRequest): Task[Unit]
  def login(request: LoginRequest): Task[JwtResponse]
}

class AuthServiceImpl(
    userRepo: UserRepository,
    jwtService: JwtService,
) extends AuthService {
  override def register(request: CreateUserRequest): Task[UserResponse] =
    HashingUtil.hash(request.password).flatMap { hash =>
      val data = UserData(
        userId = UUID.randomUUID(),
        login = request.login,
        email = request.email,
        phone = request.phone,
        passwordHash = hash,
      )
      userRepo.createUser(data).flatMap {
        case true => ZIO.succeed(data.toResponse)
        case false => ZIO.fail(UserAlreadyExistsError(request.login))
      }
    }

  override def login(request: LoginRequest): Task[JwtResponse] = for {
    userOpt <- userRepo.getByLogin(request.login)
    user <- ZIO.fromOption(userOpt).orElseFail(UserNotFoundError(request.login))
    valid <- HashingUtil.verify(request.password, user.passwordHash)
    _ <- ZIO.when(!valid)(ZIO.fail(PasswordError(request.password)))
    token <- jwtService.generateToken(user.userId)
  } yield JwtResponse(
    token = token,
    user = user.toResponse,
  )

  override def delete(request: LoginRequest): Task[Unit] = for {
    userOpt <- userRepo.getByLogin(request.login)
    user <- ZIO.fromOption(userOpt).orElseFail(UserNotFoundError(request.login))
    valid <- HashingUtil.verify(request.password, user.passwordHash)
    _ <- ZIO.when(!valid)(ZIO.fail(PasswordError(request.password)))
    _ <- userRepo.deleteByLogin(request.login)
  } yield ()
}

object AuthServiceImpl {
  val live = ZLayer.fromFunction(new AuthServiceImpl(_, _))
}
