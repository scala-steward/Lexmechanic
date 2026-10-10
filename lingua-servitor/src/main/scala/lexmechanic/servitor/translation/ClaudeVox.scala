package lexmechanic.servitor.translation

import lexmechanic.noosphere.*
import lexmechanic.servitor.translation.Vox.given
import sttp.ai.claude.ClaudeSyncClient
import sttp.ai.claude.models.{ContentBlock, Effort, Message}
import sttp.ai.claude.requests.MessageRequest

import java.util.Base64

final class ClaudeVox(client: ClaudeSyncClient, model: String) extends Vox:
  def ask(system: String, png: Array[Byte], instruction: String): Dialogue =
    val request = MessageRequest
      .withSystem(
        model = model,
        system = system,
        messages = List(
          Message.user(
            List(
              ContentBlock.image("image/png", Base64.getEncoder.encodeToString(png)),
              ContentBlock.text(instruction)
            )
          )
        ),
        maxTokens = 16000
      )
      .withEffort(Effort.Low)
    client.createMessageAs[Dialogue](request)
