package todos.utils

import todos.models.{CreateTodoRequest, Priority, TodoItem, UserData}
import zio.*
import zio.test.Gen

import java.time.Instant
import java.util.UUID

object ToDoGenerators {

  implicit val genUUID: Gen[Any, UUID] = Gen.uuid

  implicit val genOptionString: Gen[Any, Option[String]] =
    Gen.option(Gen.alphaNumericStringBounded(5, 50).map(str => s"Todo: $str"))

  implicit val genPriority: Gen[Any, Priority] = Gen.oneOf(
    Gen.const(Priority.Low),
    Gen.const(Priority.Medium),
    Gen.const(Priority.High)
  )

  implicit val genString: Gen[Any, String] =
    Gen.alphaNumericStringBounded(5, 50)

  implicit val genTags: Gen[Any, List[String]] =
    Gen.listOfBounded(0, 5)(Gen.alphaNumericStringBounded(3, 10))

  implicit val genInstant: Gen[Any, Instant] =
    Gen
      .long(0L, 2592000L)
      .map(seconds => Instant.now().minusSeconds(seconds).truncatedTo(java.time.temporal.ChronoUnit.SECONDS))

  implicit val genTodoItem: Gen[Any, TodoItem] =
    for {
      id          <- genUUID
      userId      <- genUUID
      priority    <- genPriority
      description <- genOptionString
      isComplete  <- Gen.boolean
      createdAt   <- genInstant
      completedAt <- Gen.option(genInstant)
      tags        <- genTags
    } yield TodoItem(id, userId, description, priority, isComplete, createdAt, completedAt, tags)

  implicit val genCreateTodoRequest: Gen[Any, CreateTodoRequest] =
    for {
      userId      <- genUUID
      priority    <- genPriority
      description <- genOptionString
      createdAt   <- genInstant
      completedAt <- Gen.option(genInstant)
      tags        <- genTags
    } yield CreateTodoRequest(userId, description, priority, completedAt, tags)

  implicit val genUserData: Gen[Any, UserData] =
    for {
      userId <- genUUID
      login  <- genString
      email  <- genString
      phone  <- genString
    } yield UserData(userId, login, email, phone)

  def generate[A](implicit gen: Gen[Any, A]): UIO[A] =
    gen.sample.map(_.value).runCollect.map(_.head)

  def generateUnsafe[A](implicit gen: Gen[Any, A]): A =
    zio.Unsafe.unsafe { implicit unsafe =>
      zio.Runtime.default.unsafe.run(gen.sample.map(_.value).runCollect.map(_.head)).getOrThrow()
    }

}
