package lexmechanic.noosphere

import io.circe.{Codec, Decoder, Encoder}

def lookupCodec[A](values: Iterable[A], kind: String)(key: A => String): Codec[A] = Codec.from(
  Decoder.decodeString.emap(s => values.find(key(_).equalsIgnoreCase(s)).toRight(s"Unknown $kind: $s")),
  Encoder.encodeString.contramap(key)
)

def nameCodec[E](values: Array[E], kind: String): Codec[E] = lookupCodec(values, kind)(_.toString.toLowerCase)
