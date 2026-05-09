package todos.service

import todos.common.CommonUtilsTests
import todos.repository.userimpl.UserRepository
import todos.common.ToDoGenerators.*
import todos.errors.AppErrors.{PasswordError, UserAlreadyExistsError, UserNotFoundError}
import todos.models.{CreateUserRequest, JwtResponse, LoginRequest, UserData}
import todos.util.HashingUtil
import zio.ZIO

class AuthServiceTest extends CommonUtilsTests {

  "register" should "Success" in new Testing {
    userRepo.createUser.expects(*).returns(ZIO.succeed(true)).once()
    val result = unsafeRun(service.register(createUserRequest).exit)

    result.isSuccess shouldBe true
    result.map { res =>
      assert(res.login == createUserRequest.login)
      assert(res.email == createUserRequest.email)
      assert(res.phone == createUserRequest.phone)
    }
  }
  it should "User already exists" in new Testing {
    userRepo.createUser.expects(*).returns(ZIO.succeed(false)).once()
    checkFailure(service.register(createUserRequest))(UserAlreadyExistsError(createUserRequest.login))

  }

  "login" should "Success" in new Testing {
    userRepo.getByLogin.expects(createUserRequest.login).returns(ZIO.some(user)).once()
    jwtService.generateToken.expects(user.userId).returns(ZIO.succeed(token))
    checkSuccess(service.login(loginRequest))(JwtResponse(token, user.toResponse))
  }
  it should "User not found" in new Testing {
    userRepo.getByLogin.expects(createUserRequest.login).returns(ZIO.none).once()
    checkFailure(service.login(loginRequest))(UserNotFoundError(createUserRequest.login))
  }
  it should "Invalid password" in new Testing {
    userRepo.getByLogin.expects(createUserRequest.login).returns(ZIO.some(user)).once()
    checkFailure(service.login(loginRequest.copy(password = "incorrect")))(PasswordError("incorrect"))
  }

  "delete" should "Success" in new Testing {
    userRepo.getByLogin.expects(createUserRequest.login).returns(ZIO.some(user)).once()
    userRepo.deleteByLogin.expects(createUserRequest.login).returns(ZIO.unit).once()
    checkSuccess(service.delete(loginRequest))(())
  }
  it should "User not found" in new Testing {
    userRepo.getByLogin.expects(createUserRequest.login).returns(ZIO.none).once()
    checkFailure(service.delete(loginRequest))(UserNotFoundError(createUserRequest.login))
  }
  it should "Invalid password" in new Testing {
    userRepo.getByLogin.expects(createUserRequest.login).returns(ZIO.some(user)).once()
    checkFailure(service.delete(loginRequest.copy(password = "incorrect")))(PasswordError("incorrect"))
  }

  trait Testing {
    val createUserRequest = generateUnsafe[CreateUserRequest]
    val passwordHash = unsafeRun(HashingUtil.hash(createUserRequest.password))
    val token = "token"
    val user = generateUnsafe[UserData].copy(
      login = createUserRequest.login,
      email = createUserRequest.email,
      phone = createUserRequest.phone,
      passwordHash = passwordHash,
    )
    val loginRequest = LoginRequest(
      login = createUserRequest.login,
      password = createUserRequest.password,
    )
    val userRepo = mock[UserRepository]
    val jwtService = mock[JwtService]
    val service = new AuthServiceImpl(userRepo, jwtService)
  }

}
