package mobi.upod.android.view.cards

import android.os.Handler
import android.view.View
import android.widget.FrameLayout
import mobi.upod.android.os.Runnable
import mobi.upod.android.view.HeaderViews
import mobi.upod.android.view.animation.AnimationHelper
import mobi.upod.android.widget.card.{CardView, OnCardDismissListener}
import mobi.upod.app.R

trait CardHeaders extends HeaderViews {
  private lazy val cardContainer = createCardContainer
  protected lazy val cardHeaders = createCardHeaders
  
  protected def createCardHeaders: Seq[CardHeader] = Seq()

  private def createCardContainer: FrameLayout =
    View.inflate(context, R.layout.card_container, null).asInstanceOf[FrameLayout]

  protected def allowCardHeaders: Boolean = true
  
  override protected def hasHeaders: Boolean =
    super.hasHeaders || hasCardHeader

  private def hasCardHeader: Boolean =
    allowCardHeaders && cardHeaders.exists(_.shouldShow)

  override protected def onAddHeaders(): Unit = {
    super.onAddHeaders()
    if (hasCardHeader && showNextCard()) {
      addHeader(cardContainer)
    }
  }

  private def showNextCard(): Boolean = cardHeaders.find(_.shouldShow) match {
    case Some(cardHeader) =>
      setCurrentCard(cardHeader)
      true
    case None =>
      false
  }

  private def setCurrentCard(cardHeader: CardHeader): Unit = {
    val card = cardHeader.create(context)
    card.setOnDismissListener(OnCardDismissListener(onDismiss(cardHeader, card)))
    cardContainer.removeAllViews()
    cardContainer.addView(card)
    onHeaderLayoutChanged()
  }

  private def onDismiss(cardHeader: CardHeader, card: CardView): Unit = {

    def onAnimationFinished(): Unit = {
      if (!showNextCard()) {
        new Handler().post(Runnable(removeHeader(cardContainer)))
      }
    }

    AnimationHelper.animate(card, R.anim.card_dismiss, onAnimationFinished())
    card.resetOnDismissListener()
    cardHeader.onDismiss()
  }
}
