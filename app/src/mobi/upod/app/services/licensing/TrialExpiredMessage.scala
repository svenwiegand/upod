package mobi.upod.app.services.licensing

import android.app.Activity
import android.content.Context
import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.android.app.SimpleAlertDialogFragment
import mobi.upod.app.R

object TrialExpiredMessage {
  private val dialogTag = "trialExpiredDialog"

  def showDialog(activity: Activity)(implicit binding: BindingModule): Unit = {
    SimpleAlertDialogFragment.showFromActivity(
      activity,
      dialogTag,
      R.string.trial_expired,
      activity.getString(R.string.trial_expired_dialog),
      positiveButtonTextId = Some(R.string.license_purchase),
      positiveAction = Some(new OpenGooglePlayLicenseAction),
      neutralButtonTextId = Some(R.string.not_now),
      neutralAction = Some(new DismissLicenseExpiredMessageAction)
    )
  }

  def ensureDismissed(context: Context): Unit = {
    context match {
      case activity: Activity =>
        SimpleAlertDialogFragment.ensureDismissed(activity, dialogTag)
      case _ =>
    }
  }
}
