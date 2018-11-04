package mobi.upod.android.app.action

import android.app.Activity
import android.content.Context
import mobi.upod.android.logging.Logger

class FinishActivityAction extends Action {

  override def onFired(context: Context): Unit = context match {
    case activity: Activity => activity.finish()
    case ctx => new Logger(getClass).warn(s"Cannot call finish on non-activity context $ctx")
  }
}
