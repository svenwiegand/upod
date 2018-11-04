package mobi.upod.android.view

import android.view.View.{OnLongClickListener, OnClickListener}
import android.view.View

object LongClickListener {

  def apply(handle: View => Unit) = new OnLongClickListener {
    def onLongClick(view: View): Boolean = {
      handle(view)
      true
    }
  }

  def apply(handle: => Unit): OnLongClickListener = apply(view => handle)
}
