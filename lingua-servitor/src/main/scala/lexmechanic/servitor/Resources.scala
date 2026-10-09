package lexmechanic.servitor

object Resources:
  lazy val indexHtml: Array[Byte] = load("/web/index.html")
  lazy val mainJs: Array[Byte] = load("/web/main.js")

  private def load(path: String): Array[Byte] =
    val stream = Option(getClass.getResourceAsStream(path)).getOrElse(throw RuntimeException(s"Missing resource $path"))
    try stream.readAllBytes()
    finally stream.close()
