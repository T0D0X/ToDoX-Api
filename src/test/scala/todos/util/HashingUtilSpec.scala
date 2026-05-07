package todos.util

import zio.test.*

object HashingUtilSpec extends ZIOSpecDefault {
  def spec = suite("HashingUtilSpec")(
    test("hash should produce a non-empty string") {
      for {
        hash <- HashingUtil.hash("myPassword123")
      } yield assertTrue(hash.nonEmpty)
    },
    test("verify returns true for correct password") {
      val password = "correctPassword"
      for {
        hash <- HashingUtil.hash(password)
        valid <- HashingUtil.verify(password, hash)
      } yield assertTrue(valid)
    },
    test("verify returns false for incorrect password") {
      for {
        hash <- HashingUtil.hash("realPassword")
        valid <- HashingUtil.verify("wrongPassword", hash)
      } yield assertTrue(!valid)
    },
  )
}
