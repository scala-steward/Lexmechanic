package lexmechanic.servitor.http

import java.nio.charset.StandardCharsets.UTF_8

final case class Request(method: String, path: String, headers: Map[String, String], body: Array[Byte]):
  def bodyText: String = String(body, UTF_8)
