package lexmechanic.servitor.http

import java.nio.charset.StandardCharsets.UTF_8

final case class Response(status: Int, contentType: String, body: Array[Byte])

object Response:
  def json(status: Int, body: String): Response = Response(status, "application/json", body.getBytes(UTF_8))
  def text(status: Int, body: String): Response = Response(status, "text/plain; charset=utf-8", body.getBytes(UTF_8))
