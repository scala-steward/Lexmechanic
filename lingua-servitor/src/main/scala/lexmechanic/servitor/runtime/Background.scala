package lexmechanic.servitor.runtime

object Background:
  def run(name: String)(body: => Unit): Unit = Thread.ofPlatform().daemon().name(name).start(() => body)
