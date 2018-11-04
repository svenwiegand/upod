package mobi.upod.android.app

import android.view.View
import mobi.upod.android.widget._
import mobi.upod.app.R

class NavigationDrawerEntryAdapter(initialItems: IndexedSeq[NavigationDrawerEntry]) extends
  GroupedItemAdapter[SimpleHeader, LabeledNavigationDrawerEntryWithIcon](R.layout.drawer_section_header, R.layout.drawer_navigation_item) {

  protected type HeaderViewHolder = NavigationDrawerHeaderViewHolder

  protected type ItemViewHolder = NavigationDrawerItemViewHolder

  private var _items: IndexedSeq[NavigationDrawerEntry] = initialItems

  protected var entries: IndexedSeq[Either[SimpleHeader, LabeledNavigationDrawerEntryWithIcon]] = entriesFromItems(initialItems)

  private def entriesFromItems(items: IndexedSeq[NavigationDrawerEntry]): IndexedSeq[Either[SimpleHeader, LabeledNavigationDrawerEntryWithIcon]] = items map {
    case header: NavigationSectionHeader => Left(SimpleTextResourceHeader(header.id, header.titleId))
    case separator: NavigationSeparator => Left(Separator(separator.id))
    case item: NavigationItem => Right(item)
    case item: NavigationActionItem => Right(item)
  }

  def setItems(items: IndexedSeq[NavigationDrawerEntry]): Unit = {
    _items = items
    entries = entriesFromItems(items)
    notifyDataSetChanged()
  }

  def items: IndexedSeq[NavigationDrawerEntry] = _items

  override def hasStableIds: Boolean = true

  def getItemId(position: Int) = {
    if (position >= 0 && position < entries.size) {
      entries(position) fold (
        header => header.id,
        item => item.id
      )
    } else {
      -1
    }
  }

  protected def createHeaderViewHolder(view: View) = new NavigationDrawerHeaderViewHolder(view)

  protected def createItemViewHolder(view: View) = new NavigationDrawerItemViewHolder(view)

  def updateCounters(countersByItemId: Map[Long, Int]) {
    entries = entries map { _ fold (
      header => Left(header),
      {
        case item@(navItem: NavigationItem) => Right(navItem.withUpdatedCounter(countersByItemId.getOrElse(item.id, 0)))
        case otherItem => Right(otherItem)
      }
    ) }
    notifyDataSetChanged()
  }
}
