package todos.service

import todos.errors.AppErrors.*
import todos.models.{UserData, UserIdOrLogin}
import todos.repository.UserRepository
import zio.{Task, ZIO}

trait UserService {
  def get(request: UserIdOrLogin): Task[Option[UserData]]
  def create(user: UserData): Task[Boolean]
  def delete(request: UserIdOrLogin): Task[Unit]
}

class UserServiceImpl(userRepository: UserRepository) extends UserService {

  override def get(request: UserIdOrLogin): Task[Option[UserData]] =
    (request.userId, request.login) match {
      case (Some(userId), _) => userRepository.getByUserId(userId)
      case (_, Some(login)) => userRepository.getByLogin(login)
      case _ => ZIO.fail(RequestNotFoundError(""))
    }

  override def create(user: UserData): Task[Boolean] =
    userRepository.createUser(user)

  override def delete(request: UserIdOrLogin): Task[Unit] =
    (request.userId, request.login) match {
      case (Some(userId), _) => userRepository.deleteByUserId(userId)
      case (_, Some(login)) => userRepository.deleteByLogin(login)
      case _ => ZIO.fail(RequestNotFoundError(""))
    }
}
