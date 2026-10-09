package lexmechanic.servitor

import io.circe.{Codec, Decoder, Encoder}

def nameCodec[E](values: Array[E], kind: String): Codec[E] = Codec.from(
  Decoder.decodeString.emap(s => values.find(_.toString.equalsIgnoreCase(s)).toRight(s"Unknown $kind: $s")),
  Encoder.encodeString.contramap(_.toString.toLowerCase)
)
