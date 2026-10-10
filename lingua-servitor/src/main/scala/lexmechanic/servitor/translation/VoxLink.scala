package lexmechanic.servitor.translation

import lexmechanic.noosphere.*
import sttp.ai.claude.ClaudeSyncClient
import sttp.ai.claude.config.ClaudeConfig
import sttp.ai.openai.OpenAISyncClient
import sttp.ai.openai.config.OpenAIConfig
import sttp.tapir.Schema

trait VoxLink:
  def ask(system: String, png: Array[Byte], instruction: String): Dialogue

object VoxLink:
  given Schema[Line] = Schema.derived
  given Schema[Dialogue] = Schema.derived

  // A spent free-tier daily quota also answers 429, and every retry re-uploads the screenshot.
  private val MaxRetries = 1

  def apply(heretek: Heretek, key: String, model: String): VoxLink = heretek match
    case Heretek.Claude =>
      ClaudeVoxLink(
        ClaudeSyncClient(ClaudeConfig(apiKey = key, baseUrl = heretek.baseUrl, maxRetries = MaxRetries)),
        model
      )
    case _ =>
      OpenAiVoxLink(
        OpenAISyncClient(OpenAIConfig(apiKey = key, baseUrl = heretek.baseUrl, maxRetries = MaxRetries)),
        model
      )
