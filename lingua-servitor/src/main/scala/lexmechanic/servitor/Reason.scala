package lexmechanic.servitor

import java.nio.file.FileSystemException
import scala.util.Try

extension (e: Throwable)
  def reason: String = e match
    // Their message is just the path.
    case f: FileSystemException => s"${f.getClass.getSimpleName.stripSuffix("Exception")}: ${f.getMessage}"
    case _                      => Option(e.getMessage).getOrElse(e.toString)

def failureOf(action: => Unit): Option[String] = Try(action).failed.toOption.map(_.reason)
