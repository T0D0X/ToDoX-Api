package todos.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import todos.config.JwtConfig
import todos.errors.AppErrors.{AuthErrorBase, InvalidTokenError}
import zio.{IO, Task, ZIO, ZLayer}

import java.time.Instant
import java.util.{Date, UUID}

trait JwtService {
  def generateToken(userId: UUID): Task[String]

  def validateToken(token: String): IO[AuthErrorBase, UUID]
}

class JwtServiceImpl(jwtConfig: JwtConfig) extends JwtService {
  private val algorithm: Algorithm = Algorithm.HMAC256(jwtConfig.secret)

  private val verifier = JWT
    .require(algorithm)
    .withIssuer(jwtConfig.issuer)
    .build()

  override def generateToken(userId: UUID): Task[String] = {
    val now = Instant.now()
    val expiresAt = Instant.parse("9999-12-31T23:59:59Z") // пока будет создаваться "вечный" токен
    ZIO
      .attempt {
        JWT
          .create()
          .withIssuer(jwtConfig.issuer)
          .withSubject(userId.toString)
          .withIssuedAt(Date.from(now))
          .withExpiresAt(Date.from(expiresAt))
          .sign(algorithm)
      }
      .mapError(ex => new RuntimeException(s"JWT generation failed: ${ex.getMessage}", ex))
  }

  override def validateToken(token: String): IO[AuthErrorBase, UUID] =
    ZIO
      .attempt(verifier.verify(token))
      .map(decoded => UUID.fromString(decoded.getSubject))
      .catchAll(_ => ZIO.fail(InvalidTokenError(token)))
}

object JwtServiceImpl {
  val live = ZLayer.fromFunction(new JwtServiceImpl(_))
}
