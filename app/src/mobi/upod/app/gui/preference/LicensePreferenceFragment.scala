package mobi.upod.app.gui.preference

import android.content.Context
import mobi.upod.android.preference.SimplePreferenceFragment
import mobi.upod.app.R
import mobi.upod.app.services.licensing._


class LicensePreferenceFragment extends SimplePreferenceFragment(R.xml.pref_license) with LicenseListener {
  import LicensePreferenceFragment._

  private val licenseService = inject[LicenseService]

  protected def prefs = None

  override protected def clickActions = Map(
    PrefLicense -> new CheckLicenseAction(),
    PrefPurchase -> new OpenGooglePlayLicenseAction,
    PrefStartTrial -> new TrialAction
  )

  override def onStart(): Unit = {
    super.onStart()
    updateLicenseStatus()
    licenseService.addWeakListener(this)
  }

  override def onStop(): Unit = {
    licenseService.removeListener(this)
    super.onStop()
  }

  override def onLicenseUpdated(licensed: Boolean): Unit = {
    updateLicenseStatus()
  }

  private def updateLicenseStatus(): Unit = {
    updateLicensePreference()
    updateStartTrialPeriodPreference()
    updateTrialPeriodPreference()
  }

  private def updateLicensePreference(): Unit = {
    val (titleId, summaryId) = licenseService.licenseStatus match {
      case LicenseStatus.Unlicensed =>
        (R.string.pref_license_none, R.string.pref_license_none_summary)
      case LicenseStatus.GooglePlayLicense =>
        (R.string.pref_license_google, R.string.pref_license_google_summary)
      case LicenseStatus.GiveawayLicense =>
        (R.string.pref_license_giveaway, R.string.pref_license_giveaway_summary)
    }

    val pref = findPreference(PrefLicense)
    pref.setTitle(titleId)
    pref.setSummary(summaryId)

    findPreference(PrefPurchase).setEnabled(!licenseService.isPremium)
  }

  private def updateStartTrialPeriodPreference(): Unit = Option(findPreference(PrefStartTrial)) foreach { pref =>
    if (licenseService.isPremium || licenseService.isTrial || licenseService.isTrialExpired)
      getPreferenceScreen.removePreference(pref)
  }

  private def updateTrialPeriodPreference(): Unit = Option(findPreference(PrefTrialPeriod)) foreach { pref =>
    if (licenseService.isPremium || licenseService.canStartTrial) {
      getPreferenceScreen.removePreference(pref)
    } else {
      val summary = licenseService.remainingTrialPeriod match {
        case Some(remainingPeriod) if remainingPeriod.getStandardDays > 0 =>
          getActivity.getString(R.string.pref_license_trial_period_summary, remainingPeriod.getStandardDays: java.lang.Long)
        case Some(remainingPeriod) =>
          getActivity.getString(R.string.pref_license_trial_period_summary_epxires_soon, remainingPeriod.getStandardHours + 1: java.lang.Long)
        case _ =>
          getActivity.getString(R.string.pref_license_trial_period_summary_expired)
      }
      pref.setSummary(summary)
    }
  }

  private class TrialAction extends StartTrialAction {

    override def onFired(context: Context): Unit = {
      super.onFired(context)
      updateLicenseStatus()
    }
  }
}

private object LicensePreferenceFragment {
  val PrefLicense = "pref_license"
  val PrefPurchase = "pref_license_purchase"
  val PrefStartTrial = "pref_license_start_trial"
  val PrefTrialPeriod = "pref_license_trial_period"
}
