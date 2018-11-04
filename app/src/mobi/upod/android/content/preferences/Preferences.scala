package mobi.upod.android.content.preferences

import android.app.Application
import android.preference.PreferenceManager

abstract class Preferences(app: Application, name: Option[String] = None) {

  def preferences: Seq[Preference[_]]

  def find(key: String): Option[Preference[_]] =
    preferences.find(_.key == key)

  implicit val prefs = name match {
    case Some(explicitName) => app.getSharedPreferences(explicitName, 0)
    case None => PreferenceManager.getDefaultSharedPreferences(app)
  }

}

abstract class DefaultPreferences(app: Application, preferenceResId: Int) extends Preferences(app) {
  PreferenceManager.setDefaultValues(app, preferenceResId, false)
}
abstract class UserPreferences(app: Application) extends Preferences(app, Preferences.user)
abstract class DevicePreferences(app: Application) extends Preferences(app, Preferences.device)

object Preferences {
  val user = Some("user")
  val device = Some("device")
}
