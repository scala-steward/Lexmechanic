package lexmechanic.servitor

import lexmechanic.noosphere.*

final class Xenolinguist(vox: Vox):
  // Claude's image limits, used for every provider: it downscales larger images anyway,
  // and rejects any over 10 MB once base64-encoded.
  private val MaxEdge = 2576
  private val MaxPixels = 3_750_000
  private val MaxBytes = 10_000_000 / 4 * 3

  private def systemPrompt(captain: Gender) =
    s"""You translate dialogue from the video game Warhammer 40,000: Rogue Trader from English into Polish.
       |You receive a screenshot of the in-game dialogue window. Transcribe the English text exactly as shown,
       |then translate it into natural, literary Polish that keeps the gothic, archaic tone of the setting.
       |
       |- `speaker` is the name shown above the current line, or null if none is visible.
       |- `text` is the line being spoken; `answers` are the numbered reply options, in on-screen order.
       |- Keep answer numbering and bracketed tags such as [Dogmatic] or [Persuasion 45]; translate the tag words.
       |- Leave character, ship and planet names untranslated. Use established Polish Warhammer 40,000 terminology.
       |- The player character (the Rogue Trader, Lord Captain) is ${captain.toString.toLowerCase}.
       |  Use matching Polish grammatical gender whenever the player speaks or is addressed.""".stripMargin

  def translate(screenshot: RgbImage, captain: Gender): Dialogue =
    // A full-screen shot near the pixel limit can still exceed MaxBytes if it compresses poorly.
    val png = Iterator
      .iterate(MaxEdge)(_ * 3 / 4)
      .map(edge => Png.write(screenshot.fit(edge, MaxPixels)))
      .find(_.length <= MaxBytes)
      .get
    vox.ask(systemPrompt(captain), png, "Translate the dialogue in this screenshot.")
