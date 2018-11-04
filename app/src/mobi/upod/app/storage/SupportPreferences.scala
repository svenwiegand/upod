package mobi.upod.app.storage

import android.app.Application
import mobi.upod.android.content.preferences.{BooleanPreference, IntPreference, DefaultPreferences}
import mobi.upod.app.R

class SupportPreferences(app: Application) extends DefaultPreferences(app, R.xml.pref_support) {

  lazy val enhancedLogging = new BooleanPreference("pref_enhanced_logging")

  def preferences = Seq(
    enhancedLogging
  )
}
