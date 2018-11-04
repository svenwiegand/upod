package mobi.upod.android.widget

import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.view.View

object AdapterViewClickListener {
  def apply(handle: (AdapterView[_], View, Int, Long) => Unit) = new OnItemClickListener {
    def onItemClick(parent: AdapterView[_], view: View, position: Int, id: Long) {
      handle(parent, view, position, id)
    }
  }

  def apply(handle: Int => Unit): AdapterView.OnItemClickListener =
    apply { (adapterView, view, position, id) => handle(position) }
}
