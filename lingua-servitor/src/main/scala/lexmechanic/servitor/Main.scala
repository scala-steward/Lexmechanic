package lexmechanic.servitor

import lexmechanic.noosphere.*
import lexmechanic.servitor.desktop.ActivationRune
import lexmechanic.servitor.http.VoxCaster
import lexmechanic.servitor.image.Png
import lexmechanic.servitor.runtime.{Log, failureOf, reason}
import lexmechanic.servitor.translation.{VoxLink, Xenolinguist}
import sttp.client4.*

import java.nio.file.Paths

private val usage =
  s"""Usage:
     |  lexmechanic                       start the daemon (hotkey listener + UI server)
     |  lexmechanic trigger               ask the running daemon to translate the screen now
     |  lexmechanic translate <file.png>  translate a single screenshot and print the result
     |
     |Settings, including the AI provider and its API key, live in ${Config.file}""".stripMargin

@main def main(args: String*): Unit =
  args match
    case Seq()                  => run()
    case Seq("trigger")         => trigger()
    case Seq("translate", path) =>
      val config = Config.load()
      println(render(xenolinguist(config).translate(Png.read(Paths.get(path), None), config.mandate)))
    case _ => fail(usage, exitCode = 2)

private def run(): Unit =
  val config = Config.load()
  val servitor = Servitor(xenolinguist(config), config)
  Log.info(s"Config: ${Config.file}")
  ActivationRune.listen(servitor.onKey, servitor.hotkeyProblem)
  VoxCaster.serve(config.port, openUi(config.openCommand))(servitor.route)

private def openUi(command: List[String])(url: String): Unit =
  if command.nonEmpty then
    failureOf(ProcessBuilder(command.map(_.replace("{url}", url))*).inheritIO().start())
      .foreach(e => Log.info(s"Could not open the UI: $e"))

private def xenolinguist(config: Config): Xenolinguist =
  val heretek = config.provider
  val key = config.apiKeys
    .get(heretek.keyVariable)
    .orElse(sys.env.get(heretek.keyVariable))
    .getOrElse(fail(s"Set ${heretek.keyVariable} in apiKeys in ${Config.file}, or as an environment variable"))
  Xenolinguist(VoxLink(heretek, key, config.model.getOrElse(heretek.defaultModel)))

private def trigger(): Unit =
  val port = Config.load().port
  val backend = DefaultSyncBackend()
  val status =
    try basicRequest.post(uri"http://127.0.0.1:$port/capture").header(Api.CsrfHeader, "1").send(backend).code.code
    catch case e: Exception => fail(s"Daemon not running on port $port (${e.reason})")
    finally backend.close()
  status match
    case 200   => println("Translation started")
    case 409   => fail("Translation already in progress")
    case other => fail(s"Daemon refused the request (HTTP $other)")

private def fail(message: String, exitCode: Int = 1): Nothing =
  System.err.println(message)
  sys.exit(exitCode)

private def render(dialogue: Dialogue): String =
  val text = List(dialogue.text.translation, s"  (${dialogue.text.original})")
  val answers = dialogue.answers.flatMap(a => List("", s"  ${a.translation}", s"    (${a.original})"))
  (dialogue.speaker.map(_ + ":").toList ++ text ++ answers).mkString("\n")
