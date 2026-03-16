package todos.service

import todos.errors.AppErrors.RequestNotFoundError
import todos.models.{UpdateUserDataRequest, UserData, UserIdOrLogin}
import todos.repository.UserRepository
import todos.utils.CommonUtilsTests
import todos.utils.ToDoGenerators.*
import zio.ZIO

class UserServiceTest extends CommonUtilsTests {

  "get" should "return UserData by userId" in new Testing {
    userRepository.getByUserId.expects(user.userId).returns(ZIO.some(user))

    checkSuccess(sevice.get(userIdOrLoginEmpty.copy(userId = Some(user.userId))))(Some(user))
  }

  it should "return None by userId" in new Testing {
    userRepository.getByUserId.expects(user.userId).returns(ZIO.none)

    checkSuccess(sevice.get(userIdOrLoginEmpty.copy(userId = Some(user.userId))))(None)
  }

  it should "return UserData by login" in new Testing {
    userRepository.getByLogin.expects(user.login).returns(ZIO.some(user))

    checkSuccess(sevice.get(userIdOrLoginEmpty.copy(login = Some(user.login))))(Some(user))
  }

  it should "return None by login" in new Testing {
    userRepository.getByLogin.expects(user.login).returns(ZIO.none)

    checkSuccess(sevice.get(userIdOrLoginEmpty.copy(login = Some(user.login))))(None)
  }

  it should "return fail because doesn't have userId or Login" in new Testing {

    checkFailure(sevice.get(userIdOrLoginEmpty))(RequestNotFoundError(""))
  }
  "create" should "Success operation" in new Testing {
    userRepository.createUser.expects(user).returns(ZIO.succeed(true))

    checkSuccess(sevice.create(user))(true)
  }

  "delete" should "Success delete userData by userId" in new Testing {
    userRepository.deleteByUserId.expects(user.userId).returns(ZIO.unit)

    checkSuccess(sevice.delete(userIdOrLoginEmpty.copy(userId = Some(user.userId))))(())
  }

  it should "Success delete userData by login" in new Testing {
    userRepository.deleteByLogin.expects(user.login).returns(ZIO.unit)

    checkSuccess(sevice.delete(userIdOrLoginEmpty.copy(login = Some(user.login))))(())
  }

  it should "return fail because doesn't have userId or Login" in new Testing {

    checkFailure(sevice.delete(userIdOrLoginEmpty))(RequestNotFoundError(""))
  }
  trait Testing {
    val user                           = generateUnsafe[UserData]
    val updateUserEWmpty               = UpdateUserDataRequest.empty
    val userIdOrLoginEmpty             = UserIdOrLogin.empty
    val userRepository: UserRepository = mock[UserRepository]
    val sevice                         = new UserServiceImpl(userRepository)
  }
}
