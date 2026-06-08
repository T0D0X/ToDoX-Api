package todos.service

import todos.errors.AppErrors.{PasswordError, UserAlreadyExistsError, UserNotFoundError}
import todos.models.{CreateUserRequest, JwtResponse, LoginRequest, UserData, UserResponse}
import todos.redis.RedisCache
import todos.repository.userimpl.UserRepository
import todos.util.HashingUtil

import zio.{Task, ZIO, ZLayer}

import java.time.Duration
import java.util.UUID

trait AuthService {
  def register(request: CreateUserRequest): Task[UserResponse]
  def delete(request: LoginRequest): Task[Unit]
  def login(request: LoginRequest): Task[JwtResponse]
}

class AuthServiceImpl(
    cacheUserRepo: RedisCache[String, UserData],
    postgresUserRepo: UserRepository,
    jwtService: JwtService,
) extends AuthService {
  private val userCacheTTL = Duration.ofMinutes(5)

  private def getByLogin(login: String): Task[Option[UserData]] =
    cacheUserRepo.get(login).flatMap {
      case Some(user) => ZIO.some(user)
      case None =>
        postgresUserRepo.getByLogin(login).flatMap {
          case Some(user) =>
            cacheUserRepo.set(login, user, userCacheTTL).as(Some(user))
          case None =>
            ZIO.none
        }
    }

  override def register(request: CreateUserRequest): Task[UserResponse] =
    HashingUtil.hash(request.password).flatMap { hash =>
      val data = UserData(
        userId = UUID.randomUUID(),
        login = request.login,
        email = request.email,
        phone = request.phone,
        passwordHash = hash,
      )
      postgresUserRepo.createUser(data).flatMap {
        case true => ZIO.succeed(data.toResponse) <* cacheUserRepo.set(request.login, data, userCacheTTL)
        case false => ZIO.fail(UserAlreadyExistsError(request.login))
      }
    }

  override def login(request: LoginRequest): Task[JwtResponse] = for {
    userOpt <- getByLogin(request.login)
    user <- ZIO.fromOption(userOpt).orElseFail(UserNotFoundError(request.login))
    valid <- HashingUtil.verify(request.password, user.passwordHash)
    _ <- ZIO.when(!valid)(ZIO.fail(PasswordError(request.password)))
    token <- jwtService.generateToken(user.userId)
  } yield JwtResponse(
    token = token,
    user = user.toResponse,
  )

  override def delete(request: LoginRequest): Task[Unit] = for {
    userOpt <- getByLogin(request.login)
    user <- ZIO.fromOption(userOpt).orElseFail(UserNotFoundError(request.login))
    valid <- HashingUtil.verify(request.password, user.passwordHash)
    _ <- ZIO.when(!valid)(ZIO.fail(PasswordError(request.password)))
    _ <- postgresUserRepo.deleteByLogin(request.login) <&> cacheUserRepo.del(request.login)
  } yield ()
}

object AuthServiceImpl {
  val live = ZLayer.fromFunction(new AuthServiceImpl(_, _, _))
}
