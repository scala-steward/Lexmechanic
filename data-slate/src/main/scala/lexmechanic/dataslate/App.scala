package lexmechanic.dataslate

import com.raquo.airstream.web.WebStorageVar
import com.raquo.laminar.api.L.*
import io.circe.parser.decode
import lexmechanic.noosphere.*
import org.scalajs.dom

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

object App:
  private val server = Var(Option.empty[State])
  private val pending = Var(0)
  private val postAnswered = EventBus[Unit]()
  private val showEnglish = WebStorageVar.localStorage("lexmechanic.english", syncOwner = None).bool(default = true)

  private val state: Signal[Option[State]] = server.signal.distinct

  private val hotkey: Signal[Option[HotkeyState]] = state.map(_.map(_.hotkey))

  private val status: Signal[ServitorStatus] = state.combineWith(pending.signal).map {
    case (None, _)                                     => ServitorStatus.Offline
    case (Some(s), requests) if s.busy || requests > 0 => ServitorStatus.Busy
    case (Some(s), _) if s.error.isDefined             => ServitorStatus.Failed
    case _                                             => ServitorStatus.Ready
  }

  private val actionsDisabled = status.map(s => s == ServitorStatus.Busy || s == ServitorStatus.Offline)

  private val polling: EventStream[Option[State]] =
    // Restarting on each POST answer drops an in-flight poll, which may carry the state from before the POST.
    EventStream.merge(EventStream.periodic(600), postAnswered.events).flatMapSwitch { _ =>
      FetchStream.get("/state").map(decode[State](_).toOption).recover { case _ => Some(None) }
    }

  def post(path: String, body: String = ""): Future[Boolean] =
    val init = new dom.RequestInit {}
    init.method = dom.HttpMethod.POST
    init.headers = js.Dictionary(Api.CsrfHeader -> "1")
    init.body = body
    pending.update(_ + 1)
    val result =
      for
        response <- dom.fetch(path, init).toFuture
        text <- response.text().toFuture
      yield
        decode[State](text).foreach(s => server.set(Some(s)))
        postAnswered.emit(())
        response.ok
    result.onComplete(_ => pending.update(_ - 1))
    result

  def main(args: Array[String]): Unit =
    renderOnDomContentLoaded(dom.document.getElementById("app"), view)

  private def view = div(
    cls := "shell",
    cls("show-en") <-- showEnglish.signal,
    polling --> server.writer,
    header,
    mainView,
    Calibration.overlay(state.map(_.flatMap(_.region))),
    child.maybe <-- state.map(_.flatMap(_.error)).splitOption((_, message) => div(cls := "error", text <-- message))
  )

  private def header = headerTag(
    div(cls := "title", img(src := "/emblem.svg", alt := ""), "Lexmechanic"),
    div(
      cls := "status",
      dataAttr("state") <-- status.map(_.toString.toLowerCase),
      span(cls := "dot"),
      span(text <-- status.map(_.label))
    ),
    span(cls := "hotkey-label", "Hotkey"),
    button(
      dataAttr("learning") <-- hotkey.map(_.exists(_.learning).toString),
      disabled <-- hotkey.map(_.forall(_.unavailable.isDefined)),
      title <-- hotkey.map(_.fold("") { h =>
        val hint = if h.learning then "Click to cancel" else "Click, then press the mouse button or key to use"
        h.unavailable.getOrElse(hint)
      }),
      text <-- hotkey.map(
        _.fold("…")(h => if h.learning then "Press a button…" else h.button.fold("None")(EvdevKeys.name))
      ),
      onClick(_.sample(hotkey)) --> { h => post(if h.exists(_.learning) then "/hotkey/cancel" else "/hotkey/learn") }
    ),
    button(
      text <-- showEnglish.signal.map(if _ then "Hide original" else "Show original"),
      onClick --> { _ => showEnglish.update(!_) }
    ),
    button(
      "Calibrate",
      disabled <-- actionsDisabled,
      onClick --> { _ => post("/calibrate").foreach(ok => if ok then Calibration.open()) }
    ),
    button(
      "Translate",
      disabled <-- actionsDisabled,
      onClick --> { _ => post("/capture") }
    )
  )

  private def mainView = mainTag(
    child.maybe <-- hotkey
      .map(_.flatMap(_.unavailable))
      .splitOption((_, reason) => div(cls := "hint", text <-- reason.map(r => s"Hotkey unavailable: $r"))),
    child(
      div(
        cls := "hint",
        "No region calibrated yet - the whole screen is sent. Use Calibrate to select the dialogue window."
      )
    ) <-- state.map(_.exists(_.region.isEmpty)),
    child(div(cls := "empty", "Press the hotkey while a dialogue is on screen.")) <-- state.map(
      _.exists(_.history.isEmpty)
    ),
    entries(_.take(1)),
    child(div(cls := "history-title", "Earlier")) <-- state.map(_.exists(_.history.sizeIs > 1)),
    sectionTag(
      cls := "history",
      entries(_.drop(1))
    )
  )

  private def entries(select: List[Entry] => List[Entry]) =
    children <-- state.map(_.toList.flatMap(s => select(s.history))).split(_.id)((_, entry, _) => renderEntry(entry))

  private def renderEntry(entry: Entry) =
    val dialogue = entry.dialogue
    articleTag(
      cls := "entry",
      dialogue.speaker.map(div(cls := "speaker", _)),
      p(cls := "pl", dialogue.text.polish),
      p(cls := "en", dialogue.text.english),
      Option.when(dialogue.answers.nonEmpty)(
        ol(dialogue.answers.map(a => li(div(cls := "pl", a.polish), div(cls := "en", a.english))))
      )
    )
