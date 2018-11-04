package mobi.upod.app.services.licensing

import android.content.Context
import mobi.upod.android.app.action.ActionState.ActionState
import mobi.upod.android.app.action.{Action, ActionState}
import mobi.upod.android.widget.Toast
import mobi.upod.app.{AppInjection, R}

class StartTrialAction extends Action with AppInjection {
  private def licenseService = inject[LicenseService]

  override def state(context: Context): ActionState =
    if (licenseService.canStartTrial) ActionState.enabled else ActionState.gone

  override def onFired(context: Context): Unit = {
    licenseService.startTrial()
    Toast.show(context, R.string.trial_started)
  }
}
