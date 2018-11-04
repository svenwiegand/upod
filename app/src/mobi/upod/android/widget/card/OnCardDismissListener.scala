package mobi.upod.android.widget.card

trait OnCardDismissListener {

  def onDismiss(card: CardView): Unit
}

object OnCardDismissListener {

  def apply(handle: => Unit) = new OnCardDismissListener {

    override def onDismiss(card: CardView): Unit = handle
  }
}