package mobi.upod.app.services.licensing

import android.app.Activity
import android.content.Context
import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import mobi.upod.android.app.SimpleAlertDialogFragment
import mobi.upod.android.app.action.ActionState.ActionState
import mobi.upod.android.app.action.{Action, ActionState}
import mobi.upod.app.R

class StartTrialConfirmationAction(implicit val bindingModule: BindingModule) extends Action with Injectable {
  private val licenseService = inject[LicenseService]

  override def state(context: Context): ActionState =
    if (licenseService.canStartTrial) ActionState.enabled else ActionState.gone

  override def onFired(context: Context): Unit = context match {
    case activity: Activity => showDialog(activity)
    case _ => // ignore
  }

  private def showDialog(activity: Activity): Unit = {
    SimpleAlertDialogFragment.showFromActivity(
      activity,
      SimpleAlertDialogFragment.defaultTag,
      R.string.trial_start_title,
      activity.getString(R.string.trial_start_details),
      positiveButtonTextId = Some(R.string.yes),
      positiveAction = Some(new StartTrialAction),
      negativeButtonTextId = Some(R.string.no)
    )
  }
}
