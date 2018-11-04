package mobi.upod.android.widget

import android.view.View
import android.widget.TextView

class SimpleHeaderViewHolder(view: View) extends ViewHolder[SimpleHeader] {

  def setItem(position: Int, item: SimpleHeader) {
    view.asInstanceOf[TextView].setText(item.text(view.getContext))
  }
}
