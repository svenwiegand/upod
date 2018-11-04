package mobi.upod.android.app

import android.view.View
import mobi.upod.android.view.Helpers.RichView
import mobi.upod.android.widget.{SimpleHeader, ViewHolder}
import mobi.upod.app.R

class NavigationDrawerHeaderViewHolder(view: View) extends ViewHolder[SimpleHeader]  {
  private val textView = view.childTextView(R.id.text)

  def setItem(position: Int, item: SimpleHeader): Unit = item.text(view.getContext) match {
    case null => showDivider()
    case text => showText(text)
  }

  private def showText(text: CharSequence): Unit = {
    textView.setText(text)
    textView.show()
  }

  private def showDivider(): Unit = {
    textView.hide()
  }
}
