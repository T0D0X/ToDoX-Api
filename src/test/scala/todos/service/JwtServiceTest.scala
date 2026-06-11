package todos.service

import todos.common.CommonUtilsTests
import todos.config.JwtConfig
import todos.errors.AppErrors.InvalidTokenError

import java.util.UUID

import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.JWT

class JwtServiceTest extends CommonUtilsTests {
  private val validConfig = JwtConfig(
    secret = "test-secret-key",
    issuer = "test-issuer",
    ttl = 3600L,
  )
  private val validService = new JwtServiceImpl(validConfig)

  "generateToken" should "return a non-empty string" in {
    val userId = UUID.randomUUID()
    val token = unsafeRun(validService.generateToken(userId))
    token should not be empty
  }

  it should "produce a token that can be decoded with the same secret" in {
    val userId = UUID.randomUUID()
    val token = unsafeRun(validService.generateToken(userId))
    val decoded = JWT
      .require(Algorithm.HMAC256(validConfig.secret))
      .withIssuer(validConfig.issuer)
      .build()
      .verify(token)

    decoded.getSubject shouldBe userId.toString
  }

  "validateToken" should "return userId for a valid token" in {
    val userId = UUID.randomUUID()
    val token = unsafeRun(validService.generateToken(userId))
    val result = unsafeRun(validService.validateToken(token).either)
    result shouldBe Right(userId)
  }

  it should "fail for a fake (unsigned) token" in {
    val fakeToken =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
    val result = unsafeRun(validService.validateToken(fakeToken).either)
    result shouldBe Left(InvalidTokenError(fakeToken))
  }
}
