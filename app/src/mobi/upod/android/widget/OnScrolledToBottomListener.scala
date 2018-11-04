package mobi.upod.android.widget

import android.widget.AbsListView.OnScrollListener
import android.widget.AbsListView

class OnScrolledToBottomListener(onBottom: => Unit) extends OnScrollListener {
  private var scrolledToEnd = false

  override def onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int): Unit = {
    scrolledToEnd = totalItemCount > 0 && (firstVisibleItem + visibleItemCount) >= totalItemCount
  }

  override def onScrollStateChanged(view: AbsListView, scrollState: Int): Unit = {
    if (scrolledToEnd && scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
      onBottom
    }
  }
}
