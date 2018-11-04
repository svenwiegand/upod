package mobi.upod.android.view.cards

import android.content.Context
import mobi.upod.android.content.preferences.{Setter, BooleanPreference}
import mobi.upod.android.widget.card.{SingleChoiceCardView, CardButton, TextCardView, CardView}
import mobi.upod.app.R

class TipCardHeader(val key: String, createCard: Context => CardView) extends CardHeader {
  private val wasShown = new BooleanPreference(s"pref_tip_$key")(TipCardHeader.preferences) with Setter[Boolean]

  override def shouldShow: Boolean =
    !wasShown

  override def onDismiss(): Unit =
    wasShown := true

  override def create(context: Context): CardView =
    createCard(context)
}

object TipCardHeader {
  private lazy val preferences = mobi.upod.app.App.instance.getSharedPreferences("tips", 0)

  def resetTipStatus(): Unit = {
    val editor = preferences.edit()
    editor.clear()
    editor.commit()
  }

  def apply(key: String, createdCard: Context => CardView): TipCardHeader =
    new TipCardHeader(key, createdCard)

  def textTip(key: String, titleId: Int, textWithImagesId: Int) =
    new TipCardHeader(key, ctx => new TextCardView(ctx, titleId, textWithImagesId, CardButton.primary(ctx.getString(R.string.got_it))))

  def singleChoiceTip(
    key: String,
    titleId: Int,
    textWithImagesId: Int,
    defaultChoiceIndex: Int,
    onSave: Int => Unit,
    optionTextIds: Int*) = {
    val createCard: Context => SingleChoiceCardView = ctx => new SingleChoiceCardView(
      ctx,
      ctx.getString(titleId),
      ctx.getString(textWithImagesId),
      defaultChoiceIndex,
      onSave,
      optionTextIds.map(ctx.getString): _*
    )
    new TipCardHeader(key, createCard)
  }
}
