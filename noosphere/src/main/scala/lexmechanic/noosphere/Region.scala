package lexmechanic.noosphere

import io.circe.Codec

final case class Region(x: Int, y: Int, width: Int, height: Int) derives Codec.AsObject
