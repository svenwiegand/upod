package mobi.upod.android.app

import android.app.Activity
import mobi.upod.android.app.action.Action
import mobi.upod.app.R

object ConfirmationDialog {

  def show(activity: Activity, titleId: Int, message: CharSequence, onConfirmed: Action): Unit = SimpleAlertDialogFragment.showFromActivity(
    activity,
    SimpleAlertDialogFragment.defaultTag,
    titleId,
    message,
    positiveButtonTextId = Some(R.string.yes),
    negativeButtonTextId = Some(R.string.no),
    positiveAction = Some(onConfirmed)
  )

  def show(activity: Activity, titleId: Int, messageId: Int, onConfirmed: Action): Unit =
    show(activity, titleId, activity.getString(messageId), onConfirmed)
}
