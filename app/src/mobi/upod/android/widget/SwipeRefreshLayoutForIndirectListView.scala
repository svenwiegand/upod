package mobi.upod.android.widget

import android.content.Context
import android.support.v4.view.ViewCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.util.AttributeSet
import android.view.View
import android.widget.AbsListView

class SwipeRefreshLayoutForIndirectListView(context: Context, attrs: AttributeSet) extends SwipeRefreshLayout(context, attrs) {
  private var _listView: Option[AbsListView] = None

  def setProgressIndicatorOffset(offset: Int): Unit =
    setProgressViewEndTarget(true, mOriginalOffsetTop + offset)

  def setListView(listView: AbsListView): Unit =
    _listView = Some(listView)

  override def canChildScrollUp: Boolean = _listView match {
    case Some(listView) if listView.getVisibility == View.VISIBLE => ViewCompat.canScrollVertically(listView, -1)
    case _ => super.canChildScrollUp
  }
}
