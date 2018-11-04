package mobi.upod.app.services.licensing

import android.app.Activity
import android.content.Context
import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.android.app.SimpleAlertDialogFragment
import mobi.upod.app.R

object PremiumMessage {
  private val dialogTag = "premiumFeatureDialog"

  def showDialog(activity: Activity)(implicit bindings: BindingModule): Unit = {
    val licenseService = bindings.inject[LicenseService](None)
    val canStartTrial = licenseService.canStartTrial
    val msgId = if (canStartTrial) R.string.premium_feature_details_with_trial_option else R.string.premium_feature_details
    SimpleAlertDialogFragment.showFromActivity(
      activity,
      dialogTag,
      R.string.premium_feature,
      activity.getString(msgId),
      positiveButtonTextId = Some(R.string.action_purchase),
      positiveAction = Some(new OpenGooglePlayLicenseAction),
      neutralButtonTextId = if (canStartTrial) Some(R.string.action_start_trial) else None,
      neutralAction = if (canStartTrial) Some(new StartTrialAction) else None,
      negativeButtonTextId = Some(R.string.not_now)
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
