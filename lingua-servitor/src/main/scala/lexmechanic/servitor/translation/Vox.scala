package lexmechanic.servitor.translation

import lexmechanic.noosphere.*
import sttp.ai.claude.ClaudeSyncClient
import sttp.ai.claude.config.ClaudeConfig
import sttp.ai.openai.OpenAISyncClient
import sttp.ai.openai.config.OpenAIConfig
import sttp.tapir.Schema

trait Vox:
  def ask(system: String, png: Array[Byte], instruction: String): Dialogue

object Vox:
  given Schema[Line] = Schema.derived
  given Schema[Dialogue] = Schema.derived

  // A spent free-tier daily quota also answers 429, and every retry re-uploads the screenshot.
  private val MaxRetries = 1

  def apply(provider: Provider, key: String, model: String): Vox = provider match
    case Provider.Claude =>
      ClaudeVox(
        ClaudeSyncClient(ClaudeConfig(apiKey = key, baseUrl = provider.baseUrl, maxRetries = MaxRetries)),
        model
      )
    case _ =>
      OpenAiVox(
        OpenAISyncClient(OpenAIConfig(apiKey = key, baseUrl = provider.baseUrl, maxRetries = MaxRetries)),
        model
      )
