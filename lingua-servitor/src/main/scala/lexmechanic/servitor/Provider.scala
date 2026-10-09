package lexmechanic.servitor

import io.circe.Codec
import sttp.ai.claude.config.ClaudeConfig
import sttp.client4.*
import sttp.model.Uri

enum Provider(val baseUrl: Uri, val keyVariable: String, val defaultModel: String):
  case Gemini
      extends Provider(
        uri"https://generativelanguage.googleapis.com/v1beta/openai",
        "GEMINI_API_KEY",
        "gemini-3.5-flash-lite"
      )
  case OpenRouter
      extends Provider(uri"https://openrouter.ai/api/v1", "OPENROUTER_API_KEY", "google/gemma-4-31b-it:free")
  case Mistral extends Provider(uri"https://api.mistral.ai/v1", "MISTRAL_API_KEY", "mistral-small-latest")
  case Claude extends Provider(ClaudeConfig.DefaultBaseUrl, "ANTHROPIC_API_KEY", "claude-opus-5-5")

object Provider:
  given Codec[Provider] = nameCodec(values, "provider")
