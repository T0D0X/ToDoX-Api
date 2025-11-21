package todos.generator

import zio.*
import zio.test.Gen
import todos.models.{CreateTodoRequest, Priority, TodoItem}

import java.util.UUID
import zio.UIO

import java.time.Instant

object ToDoGenerators {

		implicit val genUUID: Gen[Any, UUID] = Gen.uuid

		implicit val genDescription: Gen[Any, Option[String]] =
				Gen.option(Gen.alphaNumericStringBounded(5, 50).map(str => s"Todo: $str"))

		implicit val genPriority: Gen[Any, Priority] = Gen.oneOf(
				Gen.const(Priority.Low),
				Gen.const(Priority.Medium),
				Gen.const(Priority.High)
		)

		implicit val genTags: Gen[Any, List[String]] =
				Gen.listOfBounded(0, 5)(Gen.alphaNumericStringBounded(3, 10))

		implicit val genInstant: Gen[Any, Instant] =
				Gen.long(0L, 2592000L).map(seconds =>
						Instant.now().minusSeconds(seconds).truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
				)


		implicit val genTodoItem: Gen[Any, TodoItem] =
				for {
						id <- genUUID
						userId <- genUUID
						priority <- genPriority
						description <- genDescription
						isComplete <- Gen.boolean
						createdAt <- genInstant
						completedAt <- Gen.option(genInstant)
						tags <- genTags
				} yield TodoItem(id, userId, description, priority, isComplete, createdAt, completedAt, tags)

		implicit val genCreateTodoRequest: Gen[Any, CreateTodoRequest] =
				for {
						userId <- genUUID
						priority <- genPriority
						description <- genDescription
						createdAt <- genInstant
						completedAt <- Gen.option(genInstant)
						tags <- genTags
				} yield CreateTodoRequest(userId, description, priority, completedAt, tags)

		def generate[A](implicit gen: Gen[Any, A]): UIO[A] =
				gen.sample.map(_.value).runCollect.map(_.head)

		def generateUnsafe[A](implicit gen: Gen[Any, A]): A =
				zio.Unsafe.unsafe { implicit unsafe =>
						zio.Runtime.default.unsafe.run(gen.sample.map(_.value).runCollect.map(_.head)).getOrThrow()
				}

}
