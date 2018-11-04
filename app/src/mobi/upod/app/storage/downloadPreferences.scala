package mobi.upod.app.storage

import android.app.Application
import mobi.upod.android.content.preferences.{Setter, EnumerationPreference, DefaultPreferences}
import mobi.upod.app.{AppUpgradeListener, R}

class DownloadPreferences(app: Application) extends DefaultPreferences(app, R.xml.pref_download) with AppUpgradeListener {

  lazy val autoAddDownloadStrategy = new EnumerationPreference(AutoAddDownloadStrategy)("pref_download_auto_add", AutoAddDownloadStrategy.Playlist) with Setter[AutoAddDownloadStrategy.AutoAddDownloadStrategy]
  lazy val autoStartDownloadStrategy = new EnumerationPreference(AutoDownloadStrategy)("pref_download_auto_start", AutoDownloadStrategy.NonMeteredConnection)

  def preferences = Seq(
    autoAddDownloadStrategy,
    autoStartDownloadStrategy
  )

  def shouldAutoAddPlaylistEpisodes: Boolean = autoAddDownloadStrategy.get match {
    case AutoAddDownloadStrategy.Library | AutoAddDownloadStrategy.Playlist => true
    case _ => false
  }

  def shouldAutoAddLibraryEpisodes: Boolean = autoAddDownloadStrategy.get match {
    case AutoAddDownloadStrategy.Library => true
    case AutoAddDownloadStrategy.NewAndLibrary => true
    case _ => false
  }

  def shouldAutoAddNewEpisodes: Boolean = autoAddDownloadStrategy.get == AutoAddDownloadStrategy.NewAndLibrary

  override def onAppUpgrade(oldVersion: Int, newVersion: Int): Unit = {
    if (oldVersion < 402 && prefs.getString("pref_download_auto_add", "") == "All") {
      val editor = prefs.edit()
      editor.putString("pref_download_auto_add", "NewAndLibrary")
      editor.commit()
    }
  }
}
