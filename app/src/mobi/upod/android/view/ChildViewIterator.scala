package mobi.upod.android.view

import android.view.{View, ViewGroup}

class ChildViewIterator(group: ViewGroup) extends Iterator[View] {
  private var index = -1

  def hasNext: Boolean = index < (group.getChildCount - 1)

  def next(): View = {
    index += 1
    group.getChildAt(index)
  }
}
