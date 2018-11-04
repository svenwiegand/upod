package mobi.upod.android.app

import android.support.v7.app.ActionBarActivity
import android.view.ActionMode

trait ActionModeTracking extends ActionBarActivity {
  private var actionMode: Option[ActionMode] = None

  override def onActionModeStarted(mode: ActionMode) {
    actionMode = Some(mode)
    super.onActionModeStarted(mode)
  }

  override def onActionModeFinished(mode: ActionMode) {
    actionMode = None
    super.onActionModeFinished(mode)
  }

  protected def ensureActionModeFinished() {
    actionMode.foreach(_.finish())
  }
}
