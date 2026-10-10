package lexmechanic.servitor

import io.circe.Codec
import lexmechanic.noosphere.nameCodec

enum Gender:
  case Male, Female

object Gender:
  given Codec[Gender] = nameCodec(values, "gender")
