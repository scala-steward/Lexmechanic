package lexmechanic.dataslate.i18n

import lexmechanic.noosphere.Language
import org.scalajs.dom

object BrowserLocale:
  private val tags: List[String] = Option(dom.window.navigator.languages).fold(Nil)(_.toList)

  val language: Language = tags.iterator.flatMap(Language.forTag).nextOption().getOrElse(Language.fallback)

  val interface: Messages = tags.iterator.flatMap(Messages.forTag).nextOption().getOrElse(English)
