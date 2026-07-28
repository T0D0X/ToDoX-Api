package todos.config

import zio.http.Method.*

class CorsAllowedConfigSpec extends CommonReaderConfigTests[CorsAllowedConfig] {
  successTest(
    """
      |{
      | origins = ["https://myapp.com", "https://admin.myapp.com"]
      | methods = ["GET", "POST", "PUT", "DELETE", "OPTIONS"]
      | headers = ["Authorization", "Content-Type", "X-Request-ID"]
      | credentials = true
      |}
      |""".stripMargin,
    CorsAllowedConfig(
      origins = Set("https://myapp.com", "https://admin.myapp.com"),
      methods = Set(GET, PUT, POST, DELETE, OPTIONS),
      headers = Set("Authorization", "Content-Type", "X-Request-ID"),
      credentials = true,
    ),
  )
}
