package mobi.upod.app.gui.podcast

import mobi.upod.android.app.action.Action
import android.content.Context
import android.app.Activity

class AddPodcastByUrlAction extends Action {

  def onFired(context: Context): Unit = context match {
    case activity: Activity =>
      AddPodcastDialogFragment.show(activity)
    case _ =>
  }
}
