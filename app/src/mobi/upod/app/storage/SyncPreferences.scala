package mobi.upod.app.storage

import android.app.Application
import mobi.upod.android.content.preferences._
import mobi.upod.app.{AppUpgradeListener, R}
import mobi.upod.util.TimeOfDay

class SyncPreferences(app: Application) extends DefaultPreferences(app, R.xml.pref_sync) with AppUpgradeListener {

  lazy val syncFrequencyInMinutes = new StringPreference("pref_sync_frequency", "120") with Setter[String]
  lazy val syncTime1 = new TimePreference("pref_sync_time1") with OptionSetter[TimeOfDay]
  lazy val syncTime2 = new TimePreference("pref_sync_time2") with OptionSetter[TimeOfDay]
  lazy val syncOnlyOnWifi = new BooleanPreference("pref_sync_only_on_wifi", true)
  lazy val cloudSyncEnabled = new BooleanPreference("pref_sync_cloud", false) with Setter[Boolean]
  lazy val showSyncNotification = new BooleanPreference("pref_sync_notification", true)

  def syncFrequency: Option[Long] = {
    val frequencyInMinutes = syncFrequencyInMinutes.get.toInt
    if (frequencyInMinutes > 0)
      Some(syncFrequencyInMinutes.get.toInt * 60 * 1000)
    else
      None
  }

  def preferences = Seq(
    syncFrequencyInMinutes,
    syncOnlyOnWifi,
    cloudSyncEnabled,
    showSyncNotification
  )

  override def onAppUpgrade(oldVersion: Int, newVersion: Int): Unit = {
    if (oldVersion < 401) {

      def deleteIntSyncFrequency(): Unit = {
        val editor = prefs.edit()
        editor.remove("pref_sync_frequency")
        editor.commit()
      }

      deleteIntSyncFrequency()
      syncFrequencyInMinutes := "120"
      cloudSyncEnabled := true
    }
  }
}
