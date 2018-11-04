package mobi.upod.android.app.action

import mobi.upod.android.app.WaitDialogFragment
import mobi.upod.app.R
import android.content.Context
import android.app.Activity

trait ActionWaitDialog extends AsyncActionHook {
  protected def waitDialogMessageId = R.string.saving

  override protected def preProcess(context: Context) {
    showWaitDialog(context)
    super.preProcess(context)
  }

  override protected def postProcess(context: Context) {
    super.postProcess(context)
    dismissWaitDialog(context)
  }

  protected def showWaitDialog(context: Context) {
    forContextActivity(context, WaitDialogFragment.show(_, waitDialogMessageId))
  }

  protected def dismissWaitDialog(context: Context) {
    forContextActivity(context, WaitDialogFragment.dismiss)
  }

  private def forContextActivity(context: Context, task: Activity => Unit) {
    context match {
      case activity: Activity => task(activity)
      case _ => // no activity, so no dialog
    }
  }
}
