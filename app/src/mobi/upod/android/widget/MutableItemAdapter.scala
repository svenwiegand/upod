package mobi.upod.android.widget

import scala.collection.mutable.ArrayBuffer
import mobi.upod.util.Collections._

abstract class MutableItemAdapter[A <: AnyRef](itemLayout: Int, initialItems: IndexedSeq[A])
  extends ItemAdapter[A](itemLayout) with StableIds {

  private var _items = ArrayBuffer(initialItems: _*)

  def items: IndexedSeq[A] = _items

  protected def itemId(item: A): Long

  override protected def itemId(position: Int): Long = if (position >= 0 && position < items.size)
    itemId(items(position))
  else
    -1

  private def updateItem(updatedItem: A, sameIdentity: (A, A) => Boolean) {
    val index = _items.indexWhere(sameIdentity(_, updatedItem)).validIndex
    index.foreach(_items.update(_, updatedItem))
  }

  protected def updateItems(updatedItems: Traversable[A], sameIdentity: (A, A) => Boolean, notifyChanged: Boolean = true) {
    updatedItems.foreach(updateItem(_, sameIdentity))
    if (notifyChanged) {
      notifyDataSetChanged()
    }
  }

  def remove(ids: Set[Long]) {
    _items = _items filterNot { item => ids.contains(itemId(item)) }
    notifyDataSetChanged()
  }

  def move(from: Int, to: Int) {
    val item = _items.remove(from)
    _items.insert(to, item)
    notifyDataSetChanged()
  }

  def setItems(items: IndexedSeq[A]) {
    _items.clear()
    _items ++= items
    notifyDataSetChanged()
  }
}
