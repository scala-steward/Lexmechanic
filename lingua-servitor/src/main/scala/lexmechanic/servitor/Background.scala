package lexmechanic.servitor

object Background:
  def run(name: String)(body: => Unit): Unit = Thread.ofPlatform().daemon().name(name).start(() => body)
