package lexmechanic.servitor

import io.circe.Codec

enum Gender:
  case Male, Female

object Gender:
  given Codec[Gender] = nameCodec(values, "gender")
