package lexmechanic.noosphere

import io.circe.Codec

final case class Entry(id: Int, dialogue: Dialogue, language: Language) derives Codec.AsObject
