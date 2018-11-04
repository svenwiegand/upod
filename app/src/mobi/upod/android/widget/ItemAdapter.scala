package mobi.upod.android.widget

import android.view.{LayoutInflater, ViewGroup, View}
import android.widget.BaseAdapter
import mobi.upod.android.view.Helpers._

abstract class ItemAdapter[A <: AnyRef](itemLayout: Int) extends BaseAdapter {
  protected type ItemViewHolder <: ViewHolder[A]

  def items: IndexedSeq[A]

  protected def createViewHolder(view: View): ItemViewHolder

  def getCount = items.size

  def getItem(position: Int): A = items(position)

  def apply(position: Int): A = getItem(position)

  def getView(position: Int, convertView: View, parent: ViewGroup): View = {
    val view = getOrCreateView(convertView, parent)
    bindView(position, items(position), view)
    view
  }

  private def bindView(position: Int, item: A, view: View): Unit = {
    view.viewHolder[ItemViewHolder] match {
      case Some(viewHolder) =>
        viewHolder.setItem(position, item)
        onBoundView(viewHolder, position, item)
      case None =>
        throw new IllegalStateException("no view holder set though there should be one")
    }
  }

  private def getOrCreateView(existingView: View, parent: ViewGroup): View =
    if (existingView != null) existingView else createView(parent)

  private def createView(parent: ViewGroup) = {
    val view = LayoutInflater.from(parent.getContext).inflate(itemLayout, parent, false)
    view.viewHolder = createViewHolder(view)
    view
  }

  protected def onBoundView(viewHolder: ItemViewHolder, position: Int, item: A): Unit = ()
}
