package mobi.upod.android.view.cards

import android.content.Context
import mobi.upod.android.widget.card.CardView

class SimpleCardHeader(show: => Boolean, createCard: Context => CardView, afterDismiss: => Unit = ()) extends CardHeader {

  override def shouldShow: Boolean = show

  override def create(context: Context): CardView = createCard(context)

  override def onDismiss(): Unit = afterDismiss
}

object SimpleCardHeader {

  def apply(show: => Boolean, createCard: Context => CardView, afterDismiss: => Unit = ()): CardHeader =
    new SimpleCardHeader(show, createCard, afterDismiss)
}