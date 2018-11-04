package mobi.upod.app.services.playback.player

import android.graphics.SurfaceTexture
import mobi.upod.app.services.playback.{PlaybackError, RemotePlaybackState, VideoSize}
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.storage.StorageProvider
import java.net.URL

trait MediaPlayer {

  import MediaPlayer._

  def reset(): Unit

  def playsLocalFiles: Boolean

  def load(storageProvider: StorageProvider, episode: EpisodeListItem, msec: Int): Unit

  def getCurrentPosition: Int

  def getDuration: Int

  def getVideoSize: VideoSize

  def pause(): Unit

  def start(): Unit

  def canSeek: Boolean

  def seekTo(msec: Int, commit: Boolean): Unit

  def stop(): Unit

  def getSurface: Option[SurfaceTexture]

  def setSurface(surface: Option[SurfaceTexture]): Unit

  def setCareForSurface(care: Boolean): Unit

  def audioFxAvailability: AudioFxAvailability.AudioFxAvailability

  def areAudioFxAvailable: Boolean =
    audioFxAvailability == AudioFxAvailability.Available

  def setPlaybackSpeedMultiplier(multiplier: Float): Unit

  def playbackSpeedMultiplier: Float

  def setRelativeVolume(volume: Float): Unit

  /** Sets the volume gain in dB */
  def setVolumeGain(gain: Float)

  /** The volume gain in dB */
  def volumeGain: Float

  def release(): Unit

  //
  // listeners
  //

  def setOnCompletionListener(listener: OnCompletionListener): Unit

  def setOnErrorListener(listener: OnErrorListener): Unit

  def setOnPreparedListener(listener: OnPreparedListener): Unit

  def setOnSeekCompleteListener(listener: OnSeekCompleteListener): Unit

  def setOnPlaybackSpeedAdjustmentListener(listener: OnAudioFxListener): Unit

  def setOnAsyncPositionUpdateListener(listener: OnAsyncPositionUpdateListener): Unit

  def setOnPlaybackCapabilitiesChangedListener(listener: OnPlaybackCapabilitiesChangedListener): Unit
  
  def setOnPlayerDisconnectedListener(listener: OnPlayerDisconnectedListener): Unit
  
  def setOnRemoteEpisodeChangedListener(listener: OnRemoteEpisodeChangedListener): Unit
  
  def setOnRemotePlayerStateChanged(listener: OnRemotePlayerStateChangedListener): Unit
}

object MediaPlayer {

  trait OnCompletionListener {
    def onCompletion(mp: MediaPlayer)
  }

  trait OnErrorListener {
    def onError(mp: MediaPlayer, error: PlaybackError): Boolean
  }

  trait OnPreparedListener {
    def onPrepared(mediaPlayer: MediaPlayer)
  }

  trait OnSeekCompleteListener {
    def onSeekComplete(mediaPlayer: MediaPlayer)
  }

  trait OnAudioFxListener {
    def onAudioFxAvailable(available: Boolean): Unit

    def onPlaybackSpeedChange(multiplier: Float): Unit

    def onVolumeGainChange(gain: Float): Unit
  }

  trait OnAsyncPositionUpdateListener {
    def onPositionUpdated(mediaPlayer: MediaPlayer, position: Int)
  }

  trait OnPlaybackCapabilitiesChangedListener {
    def onPlaybackCapabilitiesChanged(mediaPlayer: MediaPlayer)
  }
  
  trait OnPlayerDisconnectedListener {
    def onPlayerDisconnected(mediaPlayer: MediaPlayer)
  }
  
  trait OnRemoteEpisodeChangedListener {
    def onRemoteEpisodeChanged(mediaUrl: URL, state: RemotePlaybackState.RemotePlaybackState)
  }

  trait OnRemotePlayerStateChangedListener {
    def onRemoteBuffering(mediaPlayer: MediaPlayer, mediaUrl: URL)

    def onRemotePaused(mediaPlayer: MediaPlayer, mediaUrl: URL)

    def onRemotePlaying(mediaPlayer: MediaPlayer, mediaUrl: URL)

    def onRemoteStopped(mediaPlayer: MediaPlayer)
  }
}