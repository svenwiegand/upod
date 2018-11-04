package mobi.upod.android.view

import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener

object DialogClickListener {
  def apply(handle: DialogInterface => Unit) = new OnClickListener {
    def onClick(dialog: DialogInterface, which: Int) {
      handle(dialog)
    }
  }

  def apply(handle: => Unit) = new OnClickListener {
    def onClick(dialog: DialogInterface, which: Int) {
      handle
    }
  }
}
