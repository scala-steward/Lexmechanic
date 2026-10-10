package lexmechanic.servitor.translation

import lexmechanic.noosphere.*
import sttp.ai.claude.ClaudeSyncClient
import sttp.ai.claude.config.ClaudeConfig
import sttp.ai.openai.OpenAISyncClient
import sttp.ai.openai.config.OpenAIConfig
import sttp.client4.*
import sttp.model.Uri
import sttp.tapir.Schema

trait VoxLink extends AutoCloseable:
  def ask(system: String, png: Array[Byte], instruction: String): Dialogue

object VoxLink:
  given Schema[Line] = Schema.derived
  given Schema[Dialogue] = Schema.derived

  // A spent free-tier daily quota also answers 429, and every retry re-uploads the screenshot.
  private val MaxRetries = 1

  def apply(heretek: Heretek, key: String, model: String): VoxLink =
    def openAi(baseUrl: Uri) =
      OpenAiVoxLink(OpenAISyncClient(OpenAIConfig(apiKey = key, baseUrl = baseUrl, maxRetries = MaxRetries)), model)
    heretek match
      case Heretek.Gemini     => openAi(uri"https://generativelanguage.googleapis.com/v1beta/openai")
      case Heretek.OpenRouter => openAi(uri"https://openrouter.ai/api/v1")
      case Heretek.Mistral    => openAi(uri"https://api.mistral.ai/v1")
      case Heretek.Claude => ClaudeVoxLink(ClaudeSyncClient(ClaudeConfig(apiKey = key, maxRetries = MaxRetries)), model)
