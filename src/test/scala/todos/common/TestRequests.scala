package todos.common

import zio.http.{Body, Header, Method, Path, Request, URL}

object TestRequests {
  private def request(method: Method, path: String, body: Body, token: String): Request =
    Request(method = method, url = URL(Path.decode(path)), body = body)
      .addHeader(Header.Authorization.Bearer(token))

  def get(path: String, token: String): Request =
    request(Method.GET, path, Body.empty, token)

  def delete(path: String, token: String): Request =
    request(Method.DELETE, path, Body.empty, token)

  def post(path: String, body: String, token: String): Request =
    request(Method.POST, path, Body.fromString(body), token)

  def put(path: String, body: String, token: String): Request =
    request(Method.PUT, path, Body.fromString(body), token)
}
