package lexmechanic.servitor.translation

import lexmechanic.noosphere.*
import lexmechanic.servitor.translation.VoxLink.given
import sttp.ai.openai.OpenAISyncClient
import sttp.ai.openai.requests.completions.chat.ChatRequestBody.ChatBody
import sttp.ai.openai.requests.completions.chat.ChatRequestBody.ChatCompletionModel.CustomChatCompletionModel
import sttp.ai.openai.requests.completions.chat.message.Content.{ArrayContent, ContentPart, ImageUrlDetails}
import sttp.ai.openai.requests.completions.chat.message.Message

import java.util.Base64

final class OpenAiVoxLink(client: OpenAISyncClient, model: String) extends VoxLink:
  def ask(system: String, png: Array[Byte], instruction: String): Dialogue =
    val image = s"data:image/png;base64,${Base64.getEncoder.encodeToString(png)}"
    val body = ChatBody(
      model = CustomChatCompletionModel(model),
      messages = Seq(
        Message.System(system),
        Message.User(ArrayContent(Seq(ContentPart.ImageUrl(ImageUrlDetails(image)), ContentPart.Text(instruction))))
      )
    )
    client.createChatCompletionAs[Dialogue](body)
