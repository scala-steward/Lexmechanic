package lexmechanic.noosphere

import io.circe.Codec

final case class Line(english: String, polish: String) derives Codec.AsObject
