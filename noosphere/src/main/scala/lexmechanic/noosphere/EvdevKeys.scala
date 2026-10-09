package lexmechanic.noosphere

object EvdevKeys:
  private val BtnLeft = 272
  private val BtnRight = 273
  val BtnExtra = 276
  private val BtnDigitizer = 320 to 335

  private val names = Map(
    274 -> "Middle click",
    275 -> "Mouse 4 (back)",
    BtnExtra -> "Mouse 5 (forward)",
    277 -> "Mouse 6",
    278 -> "Mouse 7",
    279 -> "Mouse 8",
    70 -> "Scroll Lock",
    110 -> "Insert",
    119 -> "Pause",
    87 -> "F11",
    88 -> "F12"
  ) ++ (1 to 10).map(n => (58 + n) -> s"F$n")

  def name(code: Int): String = names.getOrElse(code, s"Key $code")

  /** Clicks and touchscreen taps are excluded: they would bind the very tap used to operate the UI. */
  def bindable(code: Int): Boolean = code != BtnLeft && code != BtnRight && !BtnDigitizer.contains(code)
