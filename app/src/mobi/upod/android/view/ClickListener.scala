package mobi.upod.android.view

import android.view.View.OnClickListener
import android.view.View

object ClickListener {

  def apply(handle: View => Unit) = new OnClickListener {
    def onClick(view: View) {
      handle(view)
    }
  }

  def apply(handle: => Unit): OnClickListener = apply(view => handle)
}
