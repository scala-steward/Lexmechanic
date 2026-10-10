package lexmechanic.noosphere

import io.circe.Codec

enum HotkeyProblem:
  case NoDevices, NoPermission

object HotkeyProblem:
  given Codec[HotkeyProblem] = nameCodec(values, "hotkey problem")
