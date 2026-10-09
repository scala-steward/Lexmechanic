package lexmechanic.noosphere

import io.circe.Codec

final case class State(
    busy: Boolean,
    error: Option[String],
    region: Option[Region],
    hotkey: HotkeyState,
    history: List[Entry]
) derives Codec.AsObject
