package mobi.upod.android.app

import mobi.upod.android.widget.GroupViewHolder
import android.view.View
import mobi.upod.app.R
import mobi.upod.android.view.Helpers.RichView

class NavigationDrawerItemViewHolder(view: View) extends GroupViewHolder[LabeledNavigationDrawerEntryWithIcon]  {

  private val iconView = view.childImageView(R.id.icon)
  private val textView = view.childTextView(R.id.title)
  private val counterView = view.childTextView(R.id.counter)

  def setItem(position: Int, item: LabeledNavigationDrawerEntryWithIcon) {
    iconView.setImageResource(item.iconId)
    textView.setText(item.titleId)

    item match {
      case i: NavigationItem =>
        counterView.setText(i.counter.toString)
        counterView.show(i.counter > 0)
      case _ => // ignore
        counterView.hide()
    }
  }
}
