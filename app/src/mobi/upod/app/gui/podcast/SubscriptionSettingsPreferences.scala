package mobi.upod.app.gui.podcast

import android.app.Application
import mobi.upod.android.content.preferences._
import mobi.upod.app.R

private[podcast] class SubscriptionSettingsPreferences(app: Application) extends DefaultPreferences(app, R.xml.subscription_settings) {

  lazy val autoAddToPlaylist = new BooleanPreference("subscription_settings_auto_add_to_playlist") with Setter[Boolean]
  lazy val autoAddEpisodes = new BooleanPreference("subscription_settings_auto_add") with Setter[Boolean]
  lazy val limitNumberOfEpisodes = new BooleanPreference("subscription_settings_limit_number_of_episodes") with Setter[Boolean]
  lazy val maxKeptEpisodes = new IntPreference("subscription_settings_max_kept_episodes", 1) with Setter[Int]
  lazy val autoDownload = new BooleanPreference("subscription_settings_auto_download") with Setter[Boolean]
  lazy val playbackSpeed = new FloatPreference("subscription_settings_playback_speed", 1f) with Setter[Float]
  lazy val volumeGain = new FloatPreference("subscription_settings_volume_gain", 0f) with Setter[Float]

  def preferences: Seq[Preference[_]] = Seq(
    autoAddToPlaylist,
    autoAddEpisodes,
    limitNumberOfEpisodes,
    maxKeptEpisodes,
    autoDownload,
    playbackSpeed,
    volumeGain
  )
}
