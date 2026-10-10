package lexmechanic.noosphere

import io.circe.Codec

final case class State(
    busy: Boolean,
    error: Option[String],
    region: Option[Region],
    game: String,
    language: Option[Language],
    interfaceLanguage: Option[String],
    instructions: String,
    hotkey: HotkeyState,
    history: List[Entry]
) derives Codec.AsObject
