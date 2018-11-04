package mobi.upod.android.app

import android.app.{Activity, FragmentManager}
import android.content.Context
import mobi.upod.android.app.FragmentTransactions.RichFragmentManager

class SimpleDialogFragmentObjectWithShowMethod[A <: java.io.Serializable, B <: SimpleDialogFragment[A]](createFragment: => B, tag: String = SimpleDialogFragment.defaultTag)
  extends SimpleDialogFragmentObject[A, B](createFragment) {

  def show(fragmentManager: FragmentManager, data: A): Unit =
    apply(data).show(fragmentManager, tag)

  def show(activity: Activity, data: A): Unit =
    show(activity.getFragmentManager, data)

  def showIfActivity(context: Context, data: A): Boolean = context match {
    case activity: Activity =>
      show(activity, data)
      true
    case _ =>
      false
  }

  def ensureDismissed(fragmentManager: FragmentManager): Unit = {
    Option(fragmentManager.findFragmentByTag(tag)) foreach { dlgFragment =>
      fragmentManager.inTransaction(_.remove(dlgFragment))
    }
  }
}
