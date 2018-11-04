package mobi.upod.android.app

import android.app.{FragmentManager, DialogFragment, Dialog}
import android.os.Bundle

abstract class SimpleDialogFragment[A <: java.io.Serializable] extends DialogFragment {
  protected val dialogDataKey = "dialogData"

  def prepare(dialogData: A) {
    val args = new Bundle
    args.putSerializable(dialogDataKey, dialogData)
    setArguments(args)
  }

  protected def dialogData = getArguments.getSerializable(dialogDataKey).asInstanceOf[A]

  override def onCreateDialog(savedInstanceState: Bundle) = createDialog(dialogData)

  protected def createDialog(data: A): Dialog

  def show(fragmentManager: FragmentManager) {
    show(fragmentManager, SimpleDialogFragment.defaultTag)
  }
}

object SimpleDialogFragment {
  val defaultTag = "dialog"
}
