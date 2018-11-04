package mobi.upod.android.widget

import android.view.{LayoutInflater, ViewGroup, View}
import android.widget.BaseAdapter
import mobi.upod.android.view.Helpers._

abstract class GroupedItemAdapter[A <: AnyRef, B <: AnyRef](headerLayout: Int, itemLayout: Int) extends BaseAdapter {
  protected object ViewType {
    val header = 0
    val item = 1
  }

  protected type ItemViewHolder <: GroupViewHolder[B]
  protected type HeaderViewHolder <: ViewHolder[A]

  protected def entries: IndexedSeq[Either[A, B]]

  override def getViewTypeCount = 2

  override def getItemViewType(position: Int) = entries(position) match {
    case Left(_) => ViewType.header
    case Right(_) => ViewType.item
  }

  protected def createItemViewHolder(view: View): ItemViewHolder

  protected def createHeaderViewHolder(view: View): HeaderViewHolder

  def getCount = entries.size

  def getItem(position: Int): Either[A, B] = entries(position)

  override def areAllItemsEnabled() = false

  override def isEnabled(position: Int) = getItemViewType(position) == ViewType.item

  def getView(position: Int, convertView: View, parent: ViewGroup): View = entries(position) fold(
    header => getHeaderView(position, header, convertView, parent),
    item => getItemView(position, item, convertView, parent)
  )

  private def getHeaderView(position: Int, header: A, existingView: View, parent: ViewGroup): View = {
    bindView(headerLayout, existingView, parent, createHeaderViewHolder(_)) { viewHolder =>
      viewHolder.setItem(position, header)
    }
  }

  private def getItemView(position: Int, item: B, existingView: View, parent: ViewGroup): View = {
    require(position >= 0)
    bindView(itemLayout, existingView, parent, createItemViewHolder(_)) { viewHolder =>
      viewHolder.setItem(position, item)
      viewHolder.setGroupPosition(position == 0 || getItemViewType(position - 1) == ViewType.header)
    }
  }

  private def bindView[D <: ViewHolder[_]](layoutId: Int, existingView: View, parent: ViewGroup, createViewHolder: View => D)(initViewHolder: D => Unit): View = {
    val view = getOrCreateView(layoutId, existingView, parent, createViewHolder)
    view.viewHolder[D] match {
      case Some(viewHolder) =>
        initViewHolder(viewHolder)
      case None =>
        throw new IllegalStateException("no view holder set though there should be one")
    }
    view
  }

  private def getOrCreateView(layoutId: Int, existingView: View, parent: ViewGroup, createViewHolder: View => ViewHolder[_]): View =
    if (existingView != null) existingView else createView(layoutId, parent, createViewHolder)

  private def createView(layoutId: Int, parent: ViewGroup, createViewHolder: View => ViewHolder[_]) = {
    val view = LayoutInflater.from(parent.getContext).inflate(layoutId, parent, false)
    view.viewHolder = createViewHolder(view)
    view
  }
}
