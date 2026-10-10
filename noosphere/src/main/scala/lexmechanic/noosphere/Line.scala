package lexmechanic.noosphere

import io.circe.Codec

final case class Line(original: String, translation: String) derives Codec.AsObject
