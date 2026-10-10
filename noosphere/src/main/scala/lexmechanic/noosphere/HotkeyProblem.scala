package lexmechanic.noosphere

import io.circe.Codec

enum HotkeyProblem(val description: String):
  case NoDevices
      extends HotkeyProblem(
        "no input devices found (evdev is Linux-only) - use Translate or `lexmechanic trigger`"
      )
  case NoPermission
      extends HotkeyProblem(
        "input devices with buttons are not readable - run `sudo usermod -aG input $USER` and log in again"
      )

object HotkeyProblem:
  given Codec[HotkeyProblem] = nameCodec(values, "hotkey problem")
