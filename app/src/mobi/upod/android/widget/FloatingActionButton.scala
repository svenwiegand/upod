package mobi.upod.android.widget

import android.app.Fragment
import android.content.Context
import android.support.v4.widget.SwipeRefreshLayout
import android.util.AttributeSet
import android.view.{Gravity, ViewGroup}
import android.widget.{AbsListView, FrameLayout}
import com.melnykov.fab.FloatingActionButton.FabOnScrollListener
import mobi.upod.android.app.action.Action
import mobi.upod.android.app.{GridFragment, ListFragment}
import mobi.upod.android.graphics.Color
import mobi.upod.android.view.Helpers.{RichViewGroup, RichView}
import mobi.upod.app.R

class FloatingActionButton(protected val context: Context, protected val attrs: AttributeSet)
  extends com.melnykov.fab.FloatingActionButton(context, attrs)
  with AbstractActionButton {

  def setColor(normal: Int, pressed: Int): Unit = {
    setColorNormal(normal)
    setColorPressed(pressed)
  }

  def setColor(color: Color): Unit =
    setColor(color, color.darken(15))

  override def getBaseline: Int =
    getHeight / 2
}

object FloatingActionButton {

  def addTo(
    rootView: FrameLayout,
    listView: AbsListView,
    action: Action,
    drawableId: Int,
    descriptionId: Int,
    scrollListener: Option[AbsListView.OnScrollListener]): FloatingActionButton = {
    val layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.RIGHT)
    val margin = rootView.getContext.getResources.getDimensionPixelSize(R.dimen.fab_margin)
    layoutParams.setMargins(margin, margin, margin, margin)

    val button = new FloatingActionButton(rootView.getContext, null)
    button.setImageDrawable(rootView.getContext.getResources.getDrawable(drawableId))
    button.setContentDescription(rootView.getContext.getString(descriptionId))
    rootView.addView(button, layoutParams)
    scrollListener match {
      case Some(l) => button.attachToListView(listView, new MultiFabOnScrollListener(l))
      case _ => button.attachToListView(listView)
    }
    button.setAction(action)
    button
  }

  def addToGrid(
    fragment: GridFragment,
    action: Action,
    drawableId: Int,
    descriptionId: Int,
    scrollListener: Option[AbsListView.OnScrollListener] = None): FloatingActionButton =
    addTo(findFrameLayoutOf(fragment).get, fragment.getGridView, action, drawableId, descriptionId, scrollListener)

  def addToList(
    fragment: ListFragment,
    action: Action,
    drawableId: Int,
    descriptionId: Int,
    scrollListener: Option[AbsListView.OnScrollListener] = None): FloatingActionButton =
    addTo(findFrameLayoutOf(fragment).get, fragment.getListView, action, drawableId, descriptionId, scrollListener)

  private def findFrameLayoutOf(fragment: Fragment): Option[FrameLayout] = fragment.getView match {
    case root: FrameLayout => Some(root)
    case root: SwipeRefreshLayout => root.childViews collectFirst { case v: FrameLayout => v }
    case _ => None
  }

  private class MultiFabOnScrollListener(listener: AbsListView.OnScrollListener) extends FabOnScrollListener {

    override def onScrollStateChanged(view: AbsListView, scrollState: Int): Unit = {
      listener.onScrollStateChanged(view, scrollState)
      super.onScrollStateChanged(view, scrollState)
    }

    override def onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int): Unit = {
      listener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount)
      super.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount)
    }
  }
}