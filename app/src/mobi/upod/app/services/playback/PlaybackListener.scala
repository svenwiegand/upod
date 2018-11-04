package mobi.upod.app.services.playback

import mobi.upod.app.data.EpisodeBaseWithPlaybackInfo
import mobi.upod.media.{MediaChapter, MediaChapterTable}

trait PlaybackListener {

  def onPlaylistChanged(): Unit = {}

  def onPreparingPlayback(episode: EpisodeBaseWithPlaybackInfo): Unit = {}

  def onPlaybackStarted(episode: EpisodeBaseWithPlaybackInfo): Unit = {}

  def onPlaybackPaused(episode: EpisodeBaseWithPlaybackInfo): Unit = {}

  def onPlaybackStopped(): Unit = {}

  def onEpisodeChanged(episode: Option[EpisodeBaseWithPlaybackInfo]): Unit = {}

  def onChaptersChanged(chapters: MediaChapterTable): Unit = {}

  def onPlaybackPositionChanged(episode: EpisodeBaseWithPlaybackInfo): Unit = {}

  def onCurrentChapterChanged(chapter: Option[MediaChapter]): Unit = {}

  def onAudioEffectsAvailable(available: Boolean): Unit = {}

  def onPlaybackSpeedChanged(playbackSpeed: Float): Unit = {}

  def onVolumeGainChanged(gain: Float): Unit = {}

  def onSleepTimerModeChanged(mode: SleepTimerMode): Unit = {}

  def onEpisodeCompleted(episode: EpisodeBaseWithPlaybackInfo): Unit = {}
}
