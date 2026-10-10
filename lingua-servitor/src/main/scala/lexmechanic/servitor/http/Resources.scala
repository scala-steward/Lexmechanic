package lexmechanic.servitor.http

object Resources:
  lazy val web: Map[String, Response] = Map(
    "/" -> asset("index.html", "text/html; charset=utf-8"),
    "/main.js" -> asset("main.js", "text/javascript; charset=utf-8"),
    "/emblem.svg" -> asset("emblem.svg", "image/svg+xml")
  )

  private def asset(name: String, contentType: String): Response =
    val path = s"/web/$name"
    val stream = Option(getClass.getResourceAsStream(path)).getOrElse(throw RuntimeException(s"Missing resource $path"))
    try Response(200, contentType, stream.readAllBytes())
    finally stream.close()
