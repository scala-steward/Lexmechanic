package lexmechanic.dataslate.i18n

import lexmechanic.noosphere.HotkeyProblem

object English extends Messages:
  val name = "English"
  val lang = "en"

  val statusOffline = "Daemon offline"
  val statusBusy = "Cogitating…"
  val statusFailed = "Error"
  val statusReady = "Ready"

  val showOriginal = "Show original"
  val hideOriginal = "Hide original"
  val calibrate = "Calibrate"
  val translate = "Translate"

  val game = "Game"
  val anyGame = "Any game"
  val translateInto = "Translate into"
  val interfaceLanguage = "Interface"
  val hotkey = "Hotkey"
  val hotkeyLearnHint = "Click, then press the mouse button or key to use"
  val hotkeyCancelHint = "Click to cancel"
  val pressAButton = "Press a button…"
  val noHotkey = "None"
  def hotkeyUnavailable(problem: HotkeyProblem) = problem match
    case HotkeyProblem.NoDevices =>
      "Hotkey unavailable: no input devices found (evdev is Linux-only) - use Translate or `lexmechanic trigger`."
    case HotkeyProblem.NoPermission =>
      "Hotkey unavailable: input devices with buttons are not readable - run `sudo usermod -aG input $USER` and log in again."

  val noRegion = "No region calibrated yet - the whole screen is sent. Use Calibrate to select the dialogue window."
  val emptyHistory = "Press the hotkey while a dialogue is on screen."
  val earlier = "Earlier"

  val calibrationInstructions = "Drag over the dialogue window, then save."
  val cancel = "Cancel"
  val saveRegion = "Save region"
  val fullScreenshot = "Full screenshot"

  val middleClick = "Middle click"
  val mouseBack = "Mouse 4 (back)"
  val mouseForward = "Mouse 5 (forward)"
  def mouseButton(number: Int) = s"Mouse $number"
  def key(code: Int) = s"Key $code"
