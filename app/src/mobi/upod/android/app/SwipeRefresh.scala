package mobi.upod.android.app

import android.app.Fragment
import android.content.Context
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.view.View
import mobi.upod.android.content.Theme.context2Theme
import mobi.upod.app.gui.Theme

trait SwipeRefresh extends Fragment with SwipeRefreshable with SwipeRefreshLayout.OnRefreshListener {
  protected val swipeRefreshEnabled = true

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    super.onViewCreated(view, savedInstanceState)
    if (swipeRefreshEnabled) {
      setOnRefreshListener(this)
    }
    setSwipeRefreshColorScheme(view.getContext)
  }

  protected def setSwipeRefreshColorScheme(context: Context): Unit = {
    val theme = new Theme(context)
    val primaryColor = context.getThemeColor(android.support.v7.appcompat.R.attr.colorPrimary)
    val accentColor = context.getThemeColor(android.support.v7.appcompat.R.attr.colorAccent)
    setSwipeRefreshColorScheme(primaryColor, accentColor)
  }
}
