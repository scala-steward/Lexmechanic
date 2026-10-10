package lexmechanic.servitor.desktop

import java.nio.file.{Files, Path}
import java.util.concurrent.TimeUnit
import scala.jdk.CollectionConverters.*

object Pictcaptor:
  // A capture tool can hang on a permission prompt; without a limit the servitor would stay busy until restarted.
  private val TimeoutSeconds = 20L

  // Translation and calibration can both capture at once; screenshot tools are not built to run concurrently.
  def fullScreen(command: List[String], target: Path): Path = synchronized {
    Files.createDirectories(target.getParent)
    Files.deleteIfExists(target)
    val output = target.resolveSibling(s"${target.getFileName}.log")
    val process = ProcessBuilder(command.map(_.replace("{file}", target.toString)).asJava)
      .redirectErrorStream(true)
      .redirectOutput(output.toFile)
      .start()
    val commandLine = command.mkString(" ")
    if !process.waitFor(TimeoutSeconds, TimeUnit.SECONDS) then
      process.destroyForcibly()
      throw RuntimeException(s"Capture command timed out after ${TimeoutSeconds}s: $commandLine")
    val exit = process.exitValue()
    if exit != 0 || !Files.exists(target) then
      throw RuntimeException(s"Capture command failed (exit $exit): $commandLine\n${Files.readString(output).trim}")
    target
  }
