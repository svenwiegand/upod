package mobi.upod.app.storage

import android.app.Application
import mobi.upod.android.content.preferences._
import mobi.upod.app.R
import mobi.upod.app.storage.AudioPlayerType.AudioPlayerType
import mobi.upod.app.storage.PlaybackNotificationButtons.PlaybackNotificationButtons
import mobi.upod.util.MediaPositionFormat
import mobi.upod.util.MediaPositionFormat._

class PlaybackPreferences(app: Application) extends DefaultPreferences(app, R.xml.pref_playback) {

  lazy val mediaTimeFormat = new EnumerationPreference(MediaPositionFormat)("pref_media_position_format", MediaPositionFormat.CurrentAndDuration) with Setter[MediaPositionFormat]
  lazy val notificationButtons = new EnumerationPreference(PlaybackNotificationButtons)("pref_playback_notification_buttons", PlaybackNotificationButtons.StopPlaySkip) with Setter[PlaybackNotificationButtons]
  lazy val fastForwardSeconds = new IntPreference("pref_fast_forward_seconds", 30) with Setter[Int]
  lazy val rewindSeconds = new IntPreference("pref_rewind_seconds", 30) with Setter[Int]
  lazy val notDownloadedEpisodesPlaybackStrategy = new EnumerationPreference(NotDownloadedEpisodesPlaybackStrategy)("pref_play_not_downloaded_episodes", NotDownloadedEpisodesPlaybackStrategy.Skip)
  lazy val autoShowPlaybackViewStrategy = new EnumerationPreference(AutoShowPlaybackViewStrategy)("pref_auto_show_playback_view", AutoShowPlaybackViewStrategy.Always)
  lazy val enforceLandscapeVideo = new BooleanPreference("pref_enforce_landscape_video")
  lazy val swapJumpWindButtons = new BooleanPreference("pref_swap_jump_wind_buttons")
  lazy val skipBackChapter = new BooleanPreference("pref_skip_back_chapter")
  lazy val doubleClickToSkipBack = new BooleanPreference("pref_double_click_ff_to_skip")
  lazy val pauseWhenBecomingNoisy = new BooleanPreference("pref_pause_when_becoming_noisy", true)
  lazy val audioPlayerType = new EnumerationPreference(AudioPlayerType)("pref_audio_player", AudioPlayerType.Android) with Setter[AudioPlayerType]
  lazy val sleepTimerDuration = new LongPreference("pref_sleep_timer_duration", 0) with Setter[Long]

  def preferences = Seq(
    mediaTimeFormat,
    notificationButtons,
    fastForwardSeconds,
    rewindSeconds,
    notDownloadedEpisodesPlaybackStrategy,
    autoShowPlaybackViewStrategy,
    enforceLandscapeVideo,
    swapJumpWindButtons,
    doubleClickToSkipBack,
    pauseWhenBecomingNoisy,
    audioPlayerType,
    sleepTimerDuration
  )
}
