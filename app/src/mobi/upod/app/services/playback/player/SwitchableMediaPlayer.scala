package mobi.upod.app.services.playback.player

import android.content.Context
import android.graphics.SurfaceTexture
import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import mobi.upod.android.logging.Logging
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.services.cast.MediaRouteService
import mobi.upod.app.storage.AudioPlayerType.AudioPlayerType
import mobi.upod.app.storage.{AudioPlayerType, PlaybackPreferences, StorageProvider}

class SwitchableMediaPlayer(context: Context)(implicit val bindingModule: BindingModule)
  extends MediaPlayer
  with Injectable
  with Logging {

  import MediaPlayer._

  private lazy val mediaRouteService = inject[MediaRouteService]
  private lazy val audioPlayerPreference = inject[PlaybackPreferences].audioPlayerType
  private var player: Option[MediaPlayer] = None

  def selectPlayer(episode: EpisodeListItem): Unit = {
    val mimeType = episode.media.mimeType

    def ensureAdequateLocalPlayer(): Unit = {
      val requiredPlayerType: AudioPlayerType = {
        if (mimeType.isAudio && SonicMediaPlayer.isAvailable)
          audioPlayerPreference.get
        else
          AudioPlayerType.Android
      }

      val hasAdequatePlayer = player exists { p =>
        (requiredPlayerType == AudioPlayerType.Sonic && p.isInstanceOf[SonicMediaPlayer]) ||
        (requiredPlayerType == AudioPlayerType.Android && p.isInstanceOf[AndroidMediaPlayer])
      }

      if (!hasAdequatePlayer) {
        player.foreach(_.release())
        player = requiredPlayerType match {
          case AudioPlayerType.Android =>
            log.crashLogInfo("switching to AndroidMediaPlayer")
            Some(new AndroidMediaPlayer)
          case AudioPlayerType.Sonic =>
            log.crashLogInfo("switching to SonicMediaPlayer")
            Some(new SonicMediaPlayer(context))
        }
      }
    }

    mediaRouteService.mediaPlayer match {
      case Some(remotePlayer) =>
        log.crashLogInfo("switching to remote player")
        player = Some(remotePlayer)
      case None =>
        ensureAdequateLocalPlayer()
    }
  }

  def isPlayerSet: Boolean = player.isDefined

  def reset() = player.foreach(_.reset())

  override def playsLocalFiles: Boolean = player.map(_.playsLocalFiles).getOrElse(true)

  def load(storageProvider: StorageProvider, episode: EpisodeListItem, msec: Int): Unit =
    forPlayerCall(_.load(storageProvider, episode, msec))

  def getCurrentPosition = forPlayerCall(_.getCurrentPosition)

  def getDuration = forPlayerCall(_.getDuration)

  def getVideoSize = forPlayerCall(_.getVideoSize)

  def pause() = forPlayerCall(_.pause())

  def start() = forPlayerCall(_.start())

  override def canSeek: Boolean = player.exists(_.canSeek)

  def seekTo(msec: Int, commit: Boolean) = forPlayerCall(_.seekTo(msec, commit))

  def stop() = player.foreach(_.stop())

  def getSurface = player.map(_.getSurface).getOrElse(None)

  def setSurface(surface: Option[SurfaceTexture]) = forPlayerCall(_.setSurface(surface))

  def setCareForSurface(care: Boolean) = ()

  def audioFxAvailability = player.map(_.audioFxAvailability).getOrElse(AudioFxAvailability.NotNow)

  def setPlaybackSpeedMultiplier(multiplier: Float) = forPlayerCall(_.setPlaybackSpeedMultiplier(multiplier))

  def playbackSpeedMultiplier = player.map(_.playbackSpeedMultiplier).getOrElse(1f)

  override def setRelativeVolume(volume: Float): Unit = player.foreach(_.setRelativeVolume(volume))

  override def setVolumeGain(gain: Float): Unit = player.foreach(_.setVolumeGain(gain))

  override def volumeGain: Float = player.map(_.volumeGain).getOrElse(1f)

  def release() = {
    player.foreach(_.release())
    player = None
  }

  def setOnCompletionListener(listener: OnCompletionListener) =
    forPlayerCall(_.setOnCompletionListener(listener), ignorableIf = listener == null)

  def setOnErrorListener(listener: OnErrorListener) =
    forPlayerCall(_.setOnErrorListener(listener), ignorableIf = listener == null)

  def setOnPreparedListener(listener: OnPreparedListener) =
    forPlayerCall(_.setOnPreparedListener(listener), ignorableIf = listener == null)

  def setOnSeekCompleteListener(listener: OnSeekCompleteListener) =
    forPlayerCall(_.setOnSeekCompleteListener(listener), ignorableIf = listener == null)

  def setOnPlaybackSpeedAdjustmentListener(listener: OnAudioFxListener): Unit =
    forPlayerCall(_.setOnPlaybackSpeedAdjustmentListener(listener), ignorableIf = listener == null)

  override def setOnAsyncPositionUpdateListener(listener: OnAsyncPositionUpdateListener): Unit =
    forPlayerCall(_.setOnAsyncPositionUpdateListener(listener), ignorableIf = listener == null)

  override def setOnPlaybackCapabilitiesChangedListener(listener: OnPlaybackCapabilitiesChangedListener): Unit =
    forPlayerCall(_.setOnPlaybackCapabilitiesChangedListener(listener), ignorableIf = listener == null)

  override def setOnPlayerDisconnectedListener(listener: OnPlayerDisconnectedListener): Unit =
    forPlayerCall(_.setOnPlayerDisconnectedListener(listener), ignorableIf = listener == null)

  override def setOnRemotePlayerStateChanged(listener: OnRemotePlayerStateChangedListener): Unit =
    forPlayerCall(_.setOnRemotePlayerStateChanged(listener), ignorableIf = listener == null)

  override def setOnRemoteEpisodeChangedListener(listener: OnRemoteEpisodeChangedListener): Unit =
    forPlayerCall(_.setOnRemoteEpisodeChangedListener(listener), ignorableIf = listener == null)

  private def forPlayerCall[A](action: MediaPlayer => A): A =
    player.map(action).getOrElse(throw new IllegalStateException("You must call setMimeType() first"))

  private def forPlayerCall[A](action: MediaPlayer => Unit, ignorableIf: Boolean): Unit = player match {
    case Some(p) => action(p)
    case None if !ignorableIf => throw new IllegalStateException("You must call setMimeType() first")
    case _ => log.crashLogWarn("ignoring call on not existing player")
  }
}
