package mobi.upod.android.view.cards

import android.content.Context
import android.view.View
import mobi.upod.android.widget.card.CardView

trait CardHeader {
  def shouldShow: Boolean
  def create(context: Context): CardView
  def onDismiss(): Unit
}