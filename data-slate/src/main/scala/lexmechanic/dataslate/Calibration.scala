package lexmechanic.dataslate

import com.raquo.laminar.api.L.*
import io.circe.syntax.*
import lexmechanic.noosphere.*
import org.scalajs.dom

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

/** Positions are kept as fractions of the screenshot, so resizing the window needs no redraw. */
object Calibration:
  private final case class Box(left: Double, top: Double, width: Double, height: Double)

  private val screenshot = Var(Option.empty[String])
  private val selection = Var(Option.empty[Box])
  private var dragStart = Option.empty[(Double, Double)]
  private var image = Option.empty[dom.html.Image]

  def open(): Unit =
    selection.set(None)
    screenshot.set(Some(s"/screenshot?${js.Date.now()}"))

  private def close(): Unit = screenshot.set(None)

  private def point(e: dom.PointerEvent): Option[(Double, Double)] = image.map { img =>
    val rect = img.getBoundingClientRect()
    def clamp(v: Double) = v.max(0).min(1)
    (clamp((e.clientX - rect.left) / rect.width), clamp((e.clientY - rect.top) / rect.height))
  }

  private def save(): Unit =
    for box <- selection.now(); img <- image do
      val (w, h) = (img.naturalWidth, img.naturalHeight)
      val region = Region(
        (box.left * w).round.toInt,
        (box.top * h).round.toInt,
        (box.width * w).round.toInt,
        (box.height * h).round.toInt
      )
      App.post("/region", region.asJson.noSpaces).foreach(ok => if ok then close())

  private def percent(edge: Box => Double) = selection.signal.map(_.fold("0")(b => s"${edge(b) * 100}%"))

  def overlay(saved: Signal[Option[Region]]) = div(
    idAttr := "calibration",
    cls("open") <-- screenshot.signal.map(_.isDefined),
    headerTag(
      div(cls := "instructions", "Drag over the dialogue window, then save."),
      button("Cancel", onClick --> { _ => close() }),
      button(
        "Save region",
        disabled <-- selection.signal.map(!_.exists(b => b.width > 0.005 && b.height > 0.005)),
        onClick --> { _ => save() }
      )
    ),
    div(
      cls := "stage",
      div(
        cls := "frame",
        img(
          alt := "Full screenshot",
          draggable := false,
          src <-- screenshot.signal.map(_.getOrElse("")),
          onMountCallback(ctx => image = Some(ctx.thisNode.ref)),
          onLoad(_.sample(saved)) --> { region =>
            for r <- region; img <- image if img.naturalWidth > 0 do
              val (w, h) = (img.naturalWidth.toDouble, img.naturalHeight.toDouble)
              selection.set(Some(Box(r.x / w, r.y / h, r.width / w, r.height / h)))
          }
        ),
        div(
          cls := "selection",
          display <-- selection.signal.map(s => if s.isDefined then "block" else "none"),
          left <-- percent(_.left),
          top <-- percent(_.top),
          width <-- percent(_.width),
          height <-- percent(_.height)
        ),
        inContext { frame =>
          Seq(
            onPointerDown.preventDefault --> { e =>
              frame.ref.asInstanceOf[js.Dynamic].setPointerCapture(e.pointerId)
              dragStart = point(e)
              selection.set(None)
            },
            onPointerMove --> { e =>
              for (x0, y0) <- dragStart; (x1, y1) <- point(e) do
                selection.set(Some(Box(x0 min x1, y0 min y1, (x1 - x0).abs, (y1 - y0).abs)))
            },
            onPointerUp --> { _ => dragStart = None },
            onPointerCancel --> { _ => dragStart = None }
          )
        }
      )
    )
  )
