package mobi.upod.app.services.licensing

import mobi.upod.android.app.action.Action
import com.escalatesoft.subcut.inject.{Injectable, BindingModule}
import android.content.Context
import mobi.upod.android.app.WaitDialogFragment
import android.app.Activity
import mobi.upod.app.{App, R}

class CheckLicenseAction
  extends Action
  with LicenseListener {

  private def licenseService = App.inject[LicenseService]

  def onFired(context: Context): Unit = {
    var shouldShowWaitDialog = true
    licenseService.addListener(new LicenseListener {
      override def onLicenseUpdated(licensed: Boolean): Unit = {
        licenseService.removeListener(this)
        closeWaitDialog(context)
        shouldShowWaitDialog = false
      }
    }, false)
    licenseService.checkLicense()
    if (shouldShowWaitDialog) {
      showWaitDialog(context)
    }
  }

  private def showWaitDialog(context: Context): Unit = forContextActivity(context) { activity =>
    WaitDialogFragment.show(activity, R.string.checking_license)
  }

  private def closeWaitDialog(context: Context): Unit = forContextActivity(context) { activity =>
    WaitDialogFragment.dismiss(activity)
  }

  private def forContextActivity(context: Context)(block: Activity => Unit): Unit = context match {
    case activity: Activity if activity != null && !activity.isFinishing => block(activity)
    case _ =>
  }
}
