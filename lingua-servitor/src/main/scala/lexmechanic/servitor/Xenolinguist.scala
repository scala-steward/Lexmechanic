package lexmechanic.servitor

import lexmechanic.noosphere.*

final class Xenolinguist(vox: Vox):
  // Claude's image limits, used for every provider: it downscales larger images anyway,
  // and rejects any over 10 MB once base64-encoded.
  private val MaxEdge = 2576
  private val MaxPixels = 3_750_000
  private val MaxBytes = 10_000_000 / 4 * 3

  private def systemPrompt(config: Config) =
    val source = if config.game.isEmpty then "a video game" else s"the video game ${config.game}"
    s"""You translate dialogue from $source into ${config.targetLanguage.describe}.
       |You receive a screenshot of the in-game dialogue window. Transcribe the text exactly as shown into `original`,
       |then put a natural translation into that language, keeping the tone and style of the original, into `translation`.
       |
       |- `speaker` is the name shown above the current line, or null if none is visible.
       |- `text` is the line being spoken; `answers` are the numbered reply options, in on-screen order.
       |- Keep answer numbering and bracketed tags such as skill checks; translate the tag words.
       |- Leave the names of characters, places and ships untranslated.
       |  Use the established terminology of the game's setting in that language where it exists.
       |- The player character is ${config.captain.toString.toLowerCase}.
       |  If that language marks grammatical gender, use the matching forms whenever the player speaks or is addressed.""".stripMargin

  def translate(screenshot: RgbImage, config: Config): Dialogue =
    // A full-screen shot near the pixel limit can still exceed MaxBytes if it compresses poorly.
    val png = Iterator
      .iterate(MaxEdge)(_ * 3 / 4)
      .map(edge => Png.write(screenshot.fit(edge, MaxPixels)))
      .find(_.length <= MaxBytes)
      .get
    vox.ask(systemPrompt(config), png, "Translate the dialogue in this screenshot.")
