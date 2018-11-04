package mobi.upod.android.widget

import android.content.Context
import android.widget.{ListView, AbsListView}
import mobi.upod.android.app.action.Action

class CheckAllItemsAction(view: => AbsListView) extends Action {

  override def onFired(context: Context): Unit = {
    val v = view
    val (headerCount, footerCount) = v match {
      case listView: ListView => (listView.getHeaderViewsCount, listView.getFooterViewsCount)
      case _ => (0, 0)
    }

    for (i <- headerCount until (v.getCount - footerCount)) {
      view.setItemChecked(i, true)
    }
  }
}
