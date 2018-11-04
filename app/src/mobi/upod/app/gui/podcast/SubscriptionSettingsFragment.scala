package mobi.upod.app.gui.podcast

import android.os.Bundle
import android.preference.Preference.OnPreferenceChangeListener
import android.preference.{Preference, PreferenceFragment}
import android.support.v7.app.ActionBarActivity
import mobi.upod.android.content.IntentHelpers._
import mobi.upod.android.os.AsyncTask
import mobi.upod.app.gui.preference.PremiumPreferences
import mobi.upod.app.services.subscription.SubscriptionService
import mobi.upod.app.storage.{AsyncTransactionTask, DownloadPreferences, EpisodeDao}
import mobi.upod.app.{AppInjection, R}

private[podcast] class SubscriptionSettingsFragment
  extends PreferenceFragment
  with PremiumPreferences
  with AppInjection {

  private lazy val subscriptionService = inject[SubscriptionService]
  private lazy val downloadPreferences = inject[DownloadPreferences]
  private lazy val episodeDao = inject[EpisodeDao]
  private lazy val podcast = getActivity.getIntent.getExtra(FullPodcastSelection).get
  private lazy val preferences = new SubscriptionSettingsPreferences(app)

  override protected def premiumPreferences: Seq[CharSequence] = {
    for (i <- 0 until getPreferenceScreen.getPreferenceCount)
      yield getPreferenceScreen.getPreference(i).getKey
  }

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    getActivity.setTitle(podcast.title)
    getActivity.asInstanceOf[ActionBarActivity].getSupportActionBar.setSubtitle(R.string.action_subscription_settings)
    loadSettings()

    addPreferencesFromResource(R.xml.subscription_settings)

    findPreference(preferences.autoDownload.key).setEnabled(!downloadPreferences.shouldAutoAddNewEpisodes)
    findPreference(preferences.playbackSpeed.key).setOnPreferenceChangeListener(PlaybackSpeedChangeListener)
    findPreference(preferences.volumeGain.key).setOnPreferenceChangeListener(VolumeGainChangeListener)
  }

  override def onDestroy(): Unit = {
    applySettings()
    super.onDestroy()
  }
  
  private def loadSettings(): Unit = {
    preferences.autoAddToPlaylist := podcast.settings.autoAddToPlaylist
    preferences.autoAddEpisodes := podcast.settings.autoAddEpisodes
    preferences.limitNumberOfEpisodes := podcast.settings.maxKeptEpisodes.isDefined
    preferences.maxKeptEpisodes := podcast.settings.maxKeptEpisodes.getOrElse(1)
    preferences.autoDownload := podcast.settings.autoDownload
    preferences.playbackSpeed := podcast.settings.playbackSpeed.getOrElse(1f)
    preferences.volumeGain := podcast.settings.volumeGain.getOrElse(0f)
  }

  private def applySettings(): Unit = {
    val settings = podcast.settings.copy(
      autoAddToPlaylist = preferences.autoAddToPlaylist.get,
      autoAddEpisodes = preferences.autoAddEpisodes.get,
      maxKeptEpisodes = if (preferences.limitNumberOfEpisodes) Some(preferences.maxKeptEpisodes) else None,
      autoDownload = preferences.autoDownload,
      playbackSpeed = if (preferences.playbackSpeed.get != 1.0f) Some(preferences.playbackSpeed.get) else None,
      volumeGain = if (preferences.volumeGain.get > 0f) Some(preferences.volumeGain.get) else None
    )
    if (settings != podcast.settings) {
      AsyncTask.execute(subscriptionService.updateSettings(podcast.uri, settings))
    }
  }

  //
  // preference changes
  //

  object PlaybackSpeedChangeListener extends OnPreferenceChangeListener {
    def onPreferenceChange(preference: Preference, newValue: scala.Any): Boolean = {
      AsyncTransactionTask.execute(episodeDao.resetPlaybackSpeed(podcast.id))
      true
    }
  }

  object VolumeGainChangeListener extends OnPreferenceChangeListener {
    def onPreferenceChange(preference: Preference, newValue: scala.Any): Boolean = {
      AsyncTransactionTask.execute(episodeDao.resetVolumeGain(podcast.id))
      true
    }
  }
}
