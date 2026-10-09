package lexmechanic.noosphere

import io.circe.derivation.ConfiguredCodec

final case class Dialogue(speaker: Option[String], text: Line, answers: List[Line] = Nil) derives ConfiguredCodec
