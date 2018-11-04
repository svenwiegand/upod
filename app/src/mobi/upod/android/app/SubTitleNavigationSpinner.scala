package mobi.upod.android.app

import android.content.Context
import android.support.v7.app.ActionBar
import android.support.v7.widget.PopupMenu
import android.view.Gravity._
import android.view.ViewGroup.LayoutParams._
import android.view.{LayoutInflater, MenuItem}
import android.widget.LinearLayout
import mobi.upod.android.view.Helpers._
import mobi.upod.app.R

class SubTitleNavigationSpinner(
  spinnerContext: Context,
  popupContext: Context,
  items: IndexedSeq[CharSequence],
  listener: ActionBar.OnNavigationListener)
  extends LinearLayout(spinnerContext)
  with PopupMenu.OnMenuItemClickListener {

  private lazy val titleView = this.childTextView(R.id.title)
  private lazy val subTitleView = this.childTextView(R.id.sub_title)
  private lazy val menu = createPopupMenu
  
  init()

  private def init(): Unit = {
    setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, CENTER_VERTICAL | START))
    setOrientation(LinearLayout.VERTICAL)
    val inflater = spinnerContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE).asInstanceOf[LayoutInflater]
    inflater.inflate(R.layout.view_mode_spinner, this, true)
    this.onClick(onClick())
  }

  private def createPopupMenu: PopupMenu = {
    val m = new PopupMenu(popupContext, subTitleView, LEFT)
    items.zipWithIndex foreach { case (label, pos) => m.getMenu.add(0, 0, pos, label) }
    m.setOnMenuItemClickListener(this)
    m
  }

  def setTitle(title: CharSequence): Unit =
    titleView.setText(title)

  def setSelectedItem(position: Int): Unit =
    onItemSelected(position)

  private def onClick(): Unit =
    menu.show()

  override def onMenuItemClick(item: MenuItem): Boolean =
    onItemSelected(item.getOrder)

  protected def onItemSelected(position: Int): Boolean = {
    subTitleView.setText(items(position))
    listener.onNavigationItemSelected(position, position)
  }
}
