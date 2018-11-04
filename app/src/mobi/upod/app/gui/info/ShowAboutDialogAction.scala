package mobi.upod.app.gui.info

import android.app.Activity
import android.content.Context
import mobi.upod.android.app.action.Action

class ShowAboutDialogAction extends Action {

  override def onFired(context: Context): Unit = context match {
    case activity: Activity => showAboutDialog(activity)
    case _ =>
  }

  private def showAboutDialog(activity: Activity): Unit =
    AboutDialogFragment.show(activity)
}
