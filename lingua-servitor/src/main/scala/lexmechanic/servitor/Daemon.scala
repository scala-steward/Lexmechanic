package lexmechanic.servitor

import io.circe.parser.decode
import io.circe.syntax.*
import lexmechanic.noosphere.*

import java.nio.file.Files
import scala.util.Try

final class Daemon(xenolinguist: Xenolinguist, initial: Config):
  private val translationShot = Config.cacheDir.resolve("translation.png")
  private val calibrationShot = Config.cacheDir.resolve("calibration.png")
  private val trustedHosts = Set(s"127.0.0.1:${initial.port}", s"localhost:${initial.port}")

  private var config = initial
  private var history = List.empty[Entry]
  private var busy = false
  private var error = Option.empty[String]
  private var learning = false
  private var hotkeyUnavailable = Option.empty[HotkeyProblem]

  private def state: State = synchronized {
    State(
      busy,
      error,
      config.region,
      config.game,
      config.language,
      config.interfaceLanguage,
      HotkeyState(config.hotkey, learning, hotkeyUnavailable),
      history
    )
  }

  def hotkeyProblem(problem: Option[HotkeyProblem]): Unit = synchronized {
    hotkeyUnavailable = problem
    if problem.isDefined then learning = false
  }

  def onKey(code: Int): Unit =
    val isHotkey = synchronized {
      if learning then
        if EvdevKeys.bindable(code) then
          learning = false
          error = failureOf(updateConfig(_.copy(hotkey = Some(code))))
        false
      else config.hotkey.contains(code)
    }
    if isHotkey && !trigger() then Log.info("Hotkey ignored: translation already in progress")

  private def trigger(): Boolean =
    val started = synchronized {
      val idle = !busy
      if idle then
        busy = true
        error = None
      idle
    }
    if started then
      Background.run("translate") {
        val failure = failureOf(translateScreen())
        failure.foreach(e => Log.info(s"Error: $e"))
        synchronized {
          error = failure
          busy = false
        }
      }
    started

  private def translateScreen(): Unit =
    val current = synchronized(config)
    val screenshot = Capture.fullScreen(current.captureCommand, translationShot)
    val dialogue = xenolinguist.translate(Png.read(screenshot, current.region), current)
    synchronized {
      history = (Entry(history.headOption.fold(1)(_.id + 1), dialogue, current.targetLanguage) :: history).take(50)
    }

  private def startLearning(): Boolean = synchronized {
    learning = hotkeyUnavailable.isEmpty
    learning
  }

  private def updateConfig(change: Config => Config): Unit = synchronized {
    val updated = change(config)
    Config.save(updated)
    config = updated
  }

  def route(request: Request): Response =
    // Host check stops DNS rebinding; cross-site forms cannot set the custom header.
    if !request.headers.get("host").exists(trustedHosts) then Response.text(403, "Forbidden host")
    else if request.method == "POST" && !request.headers.contains(Api.CsrfHeader.toLowerCase) then
      Response.text(403, "Missing header")
    else endpoint(request)

  private def currentState(status: Int = 200) = Response.json(status, state.asJson.noSpaces)

  private def act(action: => Option[(Int, String)]): Response =
    val failure = Try(action).fold(e => Some(500 -> e.reason), identity)
    synchronized { error = failure.map(_._2) }
    currentState(failure.fold(200)(_._1))

  private def endpoint(request: Request): Response = (request.method, request.path) match
    case ("GET", "/state")                                       => currentState()
    case ("GET", "/screenshot") if Files.exists(calibrationShot) =>
      Response(200, "image/png", Files.readAllBytes(calibrationShot))
    case ("POST", "/capture")   => currentState(if trigger() then 200 else 409)
    case ("POST", "/calibrate") =>
      act {
        Capture.fullScreen(synchronized(config).captureCommand, calibrationShot)
        None
      }
    case ("POST", "/region") =>
      act {
        decode[Region](request.bodyText) match
          case Right(region) if region.width > 0 && region.height > 0 =>
            updateConfig(_.copy(region = Some(region)))
            None
          case _ => Some(400 -> "Invalid region")
      }
    case ("POST", "/settings") =>
      act {
        decode[Settings](request.bodyText) match
          case Right(settings) =>
            updateConfig(c =>
              c.copy(
                game = settings.game.fold(c.game)(_.trim),
                language = settings.language.orElse(c.language),
                interfaceLanguage = settings.interfaceLanguage.orElse(c.interfaceLanguage)
              )
            )
            None
          case Left(e) => Some(400 -> e.reason)
      }
    case ("POST", "/hotkey/learn")  => currentState(if startLearning() then 200 else 409)
    case ("POST", "/hotkey/cancel") =>
      synchronized { learning = false }
      currentState()
    case ("GET", path) if Resources.web.contains(path) => Resources.web(path)
    case _                                             => Response.text(404, "Not found")
