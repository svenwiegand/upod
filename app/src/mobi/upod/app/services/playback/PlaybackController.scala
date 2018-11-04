package mobi.upod.app.services.playback

import java.net.URL

import android.graphics.SurfaceTexture
import mobi.upod.android.os.AsyncObservable
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.services.playback.player.AudioFxAvailability
import mobi.upod.media.{MediaChapter, MediaChapterTable}

trait PlaybackController extends AsyncObservable[PlaybackListener] {

  def episode: Option[EpisodeListItem]

  def playingEpisode: Option[EpisodeListItem]

  def chapters: Option[MediaChapterTable]

  def canPlay: Boolean

  def canResume: Boolean

  def canPause: Boolean

  def canSeek: Boolean

  def canSkipChapter: Boolean

  def canGoBackChapter: Boolean

  def canStop: Boolean

  def idle: Boolean

  def playing: Boolean

  def paused: Boolean

  def position: Long

  def currentChapter: Option[MediaChapter]

  def videoSize: VideoSize

  def play(episodeId: Long)

  def joinRemoteSession(mediaUrl: URL, state: RemotePlaybackState.RemotePlaybackState): Unit

  def pause()

  def resume()

  def fastForward()

  def rewind()

  def seek(position: Long, commit: Boolean)

  def skipChapter()

  def goBackChapter()

  def skip()

  def stop()

  def getSurface: Option[SurfaceTexture]

  def setSurface(surface: Option[SurfaceTexture]): Unit

  def setCareForSurface(care: Boolean): Unit

  def audioFxAvailability: AudioFxAvailability.AudioFxAvailability

  def setPlaybackSpeedMultiplier(multiplier: Float): Unit

  def playbackSpeedMultiplier: Float

  def setVolumeGain(gain: Float): Unit

  def volumeGain: Float

  def sleepTimerMode: SleepTimerMode

  def startSleepTimer(mode: SleepTimerMode): Unit

  def cancelSleepTimer(): Unit
}
