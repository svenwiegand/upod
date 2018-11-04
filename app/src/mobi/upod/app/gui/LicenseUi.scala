package mobi.upod.app.gui

import android.os.Bundle
import android.support.v7.app.ActionBarActivity
import com.escalatesoft.subcut.inject.Injectable
import mobi.upod.android.app.ActivityStateHolder
import mobi.upod.android.widget.Toast
import mobi.upod.app.R
import mobi.upod.app.services.licensing.{LicenseListener, LicenseService, TrialExpiredMessage}
import mobi.upod.app.storage.InternalAppPreferences

trait LicenseUi extends ActionBarActivity with ActivityStateHolder with Injectable with LicenseListener {
  private lazy val licenseService = inject[LicenseService]
  private lazy val appPreferences = inject[InternalAppPreferences]

  override def onCreate(savedInstanceState: Bundle): Unit = {
    licenseService.checkLicense()
    super.onCreate(savedInstanceState)
  }

  override def onStart(): Unit = {
    super.onStart()
    licenseService.addWeakListener(this)
    if (!licenseService.isPremium && licenseService.isTrialExpired) {
      showTrialExpiredDialog()
    }
  }

  override def onStop(): Unit = {
    licenseService.removeListener(this)
    super.onStop()
  }

  override def onNotLicensed(): Unit = {
    super.onNotLicensed()
    if (licenseService.isTrial) {
      showRemainingTrialPeriodIfApplicable()
    } else if (licenseService.isTrialExpired) {
      showTrialExpiredDialog()
    }
  }


  override def onLicensed(): Unit = {
    super.onLicensed()
    TrialExpiredMessage.ensureDismissed(this)
  }

  private def showRemainingTrialPeriodIfApplicable(): Unit = licenseService.remainingTrialPeriod match {
    case Some(period) if period.getStandardDays > 0 =>
      Toast.show(this, getString(R.string.trial_period_remaining, period.getStandardDays: java.lang.Long))
    case Some(period) =>
      Toast.show(this, getString(R.string.trial_period_expires_soon, period.getStandardHours + 1: java.lang.Long))
    case _ =>
  }

  private def showTrialExpiredDialog(): Unit = if (state.started && appPreferences.showTrialExpiredMessage) {
    TrialExpiredMessage.ensureDismissed(this)
    TrialExpiredMessage.showDialog(this)
  }
}
