package lexmechanic.noosphere

import io.circe.derivation.ConfiguredCodec

final case class Settings(
    game: Option[String] = None,
    language: Option[Language] = None,
    interfaceLanguage: Option[String] = None,
    instructions: Option[String] = None
) derives ConfiguredCodec
