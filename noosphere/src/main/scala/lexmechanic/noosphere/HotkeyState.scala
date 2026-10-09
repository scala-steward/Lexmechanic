package lexmechanic.noosphere

import io.circe.Codec

final case class HotkeyState(button: Option[Int], learning: Boolean, unavailable: Option[String]) derives Codec.AsObject
