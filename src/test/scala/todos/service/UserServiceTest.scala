package todos.service

import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import todos.errors.AppErrors.RequestNotFoundError
import todos.models.{UpdateUserDataRequest, UserData, UserIdOrLogin}
import todos.repository.UserRepository
import todos.utils.ToDoGenerators.*
import zio.{Exit, Runtime, Unsafe, ZIO}

class UserServiceTest extends AnyFlatSpec with Matchers with MockFactory {
  private def unsafeRun[A](zio: ZIO[Any, Throwable, A]): A =
    Unsafe.unsafe { implicit unsafe =>
      Runtime.default.unsafe.run(zio).getOrThrow()
    }

  "get" should "return UserData by userId" in new Testing {
    userRepository.getByUserId.expects(user.userId).returns(ZIO.some(user))

    unsafeRun(sevice.get(userIdOrLoginEmpty.copy(userId = Some(user.userId)))) shouldBe Some(user)
  }

  it should "return None by userId" in new Testing {
    userRepository.getByUserId.expects(user.userId).returns(ZIO.none)

    unsafeRun(sevice.get(userIdOrLoginEmpty.copy(userId = Some(user.userId)))) shouldBe None
  }

  it should "return UserData by login" in new Testing {
    userRepository.getByLogin.expects(user.login).returns(ZIO.some(user))

    unsafeRun(sevice.get(userIdOrLoginEmpty.copy(login = Some(user.login)))) shouldBe Some(user)
  }

  it should "return None by login" in new Testing {
    userRepository.getByLogin.expects(user.login).returns(ZIO.none)

    unsafeRun(sevice.get(userIdOrLoginEmpty.copy(login = Some(user.login)))) shouldBe None
  }

  it should "return fail because doesn't have userId or Login" in new Testing {

    val result = unsafeRun(sevice.get(userIdOrLoginEmpty).exit)

    result.isFailure shouldBe true
    result match {
      case Exit.Failure(cause) =>
        cause.failureOption shouldBe Some(RequestNotFoundError(""))
      case Exit.Success(_) =>
        fail("Expected failure but got success")
    }
  }
  "create" should "Success operation" in new Testing {
    userRepository.createUser.expects(user).returns(ZIO.succeed(true))

    unsafeRun(sevice.create(user)) shouldBe true
  }

  "delete" should "Success delete userData by userId" in new Testing {
    userRepository.deleteByUserId.expects(user.userId).returns(ZIO.unit)

    unsafeRun(sevice.delete(userIdOrLoginEmpty.copy(userId = Some(user.userId)))) shouldBe ()
  }

  it should "Success delete userData by login" in new Testing {
    userRepository.deleteByLogin.expects(user.login).returns(ZIO.unit)

    unsafeRun(sevice.delete(userIdOrLoginEmpty.copy(login = Some(user.login)))) shouldBe ()
  }

  it should "return fail because doesn't have userId or Login" in new Testing {

    val result = unsafeRun(sevice.delete(userIdOrLoginEmpty).exit)

    result.isFailure shouldBe true
    result match {
      case Exit.Failure(cause) =>
        cause.failureOption shouldBe Some(RequestNotFoundError(""))
      case Exit.Success(_) =>
        fail("Expected failure but got success")
    }
  }
  trait Testing {
    val user                           = generateUnsafe[UserData]
    val updateUserEWmpty               = UpdateUserDataRequest.empty
    val userIdOrLoginEmpty             = UserIdOrLogin.empty
    val userRepository: UserRepository = mock[UserRepository]
    val sevice                         = new UserServiceImpl(userRepository)
  }
}
