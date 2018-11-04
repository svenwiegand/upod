package mobi.upod.android.widget.card

import android.content.Context
import mobi.upod.android.app.action.Action

case class CardButton(text: String, action: Option[Action], primary: Boolean) {

  def asPrimary: CardButton = copy(primary = true)

  def asPrimaryIf(condition: Boolean): CardButton = copy(primary = condition)
}

object CardButton {

  def apply(text: String): CardButton =
    new CardButton(text, None, false)

  def apply(textId: Int)(implicit context: Context): CardButton =
    apply(context.getString(textId))

  def apply(text: String, action: Action): CardButton =
    new CardButton(text, Some(action), false)

  def apply(textId: Int, action: Action)(implicit context: Context): CardButton =
    apply(context.getString(textId), action)

  def primary(text: String): CardButton =
    new CardButton(text, None, true)

  def primary(textId: Int)(implicit context: Context): CardButton =
    primary(context.getString(textId))

  def primary(text: String, action: Action): CardButton =
    new CardButton(text, Some(action), true)

  def primary(textId: Int, action: Action)(implicit context: Context): CardButton =
    primary(context.getString(textId), action)
}