package mobi.upod.app.gui.preference

import android.preference.PreferenceFragment
import com.escalatesoft.subcut.inject.Injectable
import mobi.upod.android.app.ListenerFragment
import mobi.upod.android.widget.Toast
import mobi.upod.app.R
import mobi.upod.app.services.licensing.{LicenseListener, LicenseService}
import mobi.upod.util.Observable

trait PremiumPreferences extends PreferenceFragment with ListenerFragment with LicenseListener with Injectable {
  private val licenseService = inject[LicenseService]

  override protected val observables: Traversable[Observable[_ >: LicenseListener]] = Seq(licenseService)

  protected def premiumPreferences: Seq[CharSequence]

  private def updatePremiumPreferences(): Unit = {
    val licensed = licenseService.isLicensed
    premiumPreferences.map(key => findPreference(key)) foreach { pref =>
      pref.setEnabled(licensed)
      if (!licensed) {
        pref.setIcon(R.drawable.ic_pref_premium_light)
      }
    }
    if (!licensed && premiumPreferences.nonEmpty) {
      Toast.show(getActivity, R.string.disabled_premium_preferences)
    }
  }

  override def onLicenseUpdated(licensed: Boolean): Unit = {
    super.onLicenseUpdated(licensed)
    updatePremiumPreferences()
  }
}
