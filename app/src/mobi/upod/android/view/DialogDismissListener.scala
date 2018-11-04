package mobi.upod.android.view

import android.content.DialogInterface
import android.content.DialogInterface.OnDismissListener

object DialogDismissListener {
  def apply(handle: DialogInterface => Unit) = new OnDismissListener {
    def onDismiss(dialog: DialogInterface) {
      handle(dialog)
    }
  }

  def apply(handle: => Unit) = new OnDismissListener {
    def onDismiss(dialog: DialogInterface) {
      handle
    }
  }
}
