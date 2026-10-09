package lexmechanic.servitor

object Log:
  def info(message: String): Unit = System.err.println(s"[lexmechanic] $message")
