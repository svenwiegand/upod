package mobi.upod.app.services.cast

import GoogleCastUtils._
import android.graphics.SurfaceTexture
import com.google.android.gms.cast.{MediaStatus, MediaMetadata, MediaInfo, RemoteMediaPlayer}
import com.google.android.gms.common.api.{Status, GoogleApiClient}
import mobi.upod.app.services.playback.{PlaybackError, RemotePlaybackState, VideoSize}
import mobi.upod.app.services.playback.player.MediaPlayer._
import mobi.upod.app.services.playback.player.AudioFxAvailability.AudioFxAvailability
import mobi.upod.app.services.playback.player.{AudioFxAvailability, MediaPlayer}
import mobi.upod.android.logging.Logging
import mobi.upod.app.storage.StorageProvider
import mobi.upod.app.data.EpisodeListItem
import com.google.android.gms.common.images.WebImage
import android.net.Uri
import java.net.URL
import scala.util.Try

private[cast] class GoogleCastMediaPlayer(mediaPlayer: RemoteMediaPlayer, apiClient: GoogleApiClient, castApi: GoogleCastDevice.CastApi)
  extends MediaPlayer
  with RemoteMediaPlayer.OnStatusUpdatedListener
  with Logging {

  private var seekable = false
  private var completed = false
  private var _currentMediaUrl: Option[URL] = None
  private var _currentState: RemotePlaybackState.RemotePlaybackState = RemotePlaybackState.Unknown
  private var loading = true

  private var onSeekCompleteListener: Option[OnSeekCompleteListener] = None
  private var onPreparedListener: Option[OnPreparedListener] = None
  private var onErrorListener: Option[OnErrorListener] = None
  private var onCompletionListener: Option[OnCompletionListener] = None
  private var onAsyncPositionUpdateListener: Option[OnAsyncPositionUpdateListener] = None
  private var onPlaybackCapabilitiesChangedListener: Option[OnPlaybackCapabilitiesChangedListener] = None
  private var onPlayerDisconnectedListener: Option[OnPlayerDisconnectedListener] = None
  private var onEpisodeChangedListener: Option[OnRemoteEpisodeChangedListener] = None
  private var onStateChangedListener: Option[OnRemotePlayerStateChangedListener] = None

  mediaPlayer.setOnStatusUpdatedListener(this)

  def connect(callback: RemoteMediaPlayer.MediaChannelResult => Unit): Unit = {
    castApi.setMessageReceivedCallbacks(apiClient, mediaPlayer.getNamespace, mediaPlayer)
    mediaPlayer.requestStatus(apiClient).onResult(callback)
  }

  def disconnect(): Unit = {
    castApi.removeMessageReceivedCallbacks(apiClient, mediaPlayer.getNamespace)
    onPlayerDisconnectedListener.foreach(_.onPlayerDisconnected(this))
  }

  def currentMediaUrl: Option[URL] = _currentMediaUrl

  def currentPlaybackState: RemotePlaybackState.RemotePlaybackState = _currentState

  //
  // media player implementation
  //

  override def reset(): Unit = ()

  override val playsLocalFiles: Boolean = false

  def load(storageProvider: StorageProvider, episode: EpisodeListItem, msec: Int): Unit = {
    loading = true

    val metadata = new MediaMetadata
    episode.podcastInfo.imageUrl.foreach(imageUrl => metadata.addImage(new WebImage(Uri.parse(imageUrl.toString))))
    metadata.putString(MediaMetadata.KEY_ARTIST, episode.podcastInfo.title)
    metadata.putString(MediaMetadata.KEY_ALBUM_ARTIST, episode.podcastInfo.title)
    metadata.putString(MediaMetadata.KEY_TITLE, episode.title)

    val mediaInfo = new MediaInfo.Builder(episode.media.url.toString).
      setContentType(episode.media.mimeType.toString).
      setStreamType(MediaInfo.STREAM_TYPE_BUFFERED).
      setStreamDuration(episode.media.duration).
      setMetadata(metadata).
      build

    _currentMediaUrl = Some(episode.media.url)
    _currentState = RemotePlaybackState.Buffering
    mediaPlayer.load(apiClient, mediaInfo, false, msec).fold(
      handleError("load", _),
      _ => {
        loading  = false
        onPreparedListener.foreach(_.onPrepared(this))
      }
    )
  }

  override def getCurrentPosition: Int =
    mediaPlayer.getApproximateStreamPosition.toInt

  override def getDuration: Int =
    mediaPlayer.getStreamDuration.toInt

  override val getVideoSize: VideoSize =
    VideoSize(0, 0)

  override def pause(): Unit = {
    _currentState = RemotePlaybackState.Paused
    mediaPlayer.pause(apiClient).onError(handleError("pause", _))
  }

  override def start(): Unit = {
    _currentState = RemotePlaybackState.Playing
    mediaPlayer.play(apiClient).onError(handleError("play", _))
  }

  override def canSeek: Boolean = seekable

  override def seekTo(msec: Int, commit: Boolean): Unit = if (commit) {
    mediaPlayer.seek(apiClient, msec).fold(
      handleError("seek", _),
      _ => {
        onSeekCompleteListener.foreach(_.onSeekComplete(this))
      }
    )
  }

  override def stop(): Unit = {
    _currentState = RemotePlaybackState.Idle
    mediaPlayer.stop(apiClient)
  }

  override def getSurface: Option[SurfaceTexture] = None

  override def setSurface(surface: Option[SurfaceTexture]): Unit = ()

  override def setCareForSurface(care: Boolean): Unit = ()

  override def audioFxAvailability: AudioFxAvailability =
    AudioFxAvailability.NotForCurrentDataSource

  override def setPlaybackSpeedMultiplier(multiplier: Float): Unit =
    throw new UnsupportedOperationException

  override def playbackSpeedMultiplier: Float =
    1f

  override def setRelativeVolume(volume: Float): Unit =
    mediaPlayer.setStreamVolume(apiClient, volume)

  override def setVolumeGain(gain: Float): Unit =
    throw new UnsupportedOperationException

  override def volumeGain: Float = 1f

  override def release(): Unit = ()

  override def setOnCompletionListener(listener: OnCompletionListener): Unit =
    onCompletionListener = Option(listener)

  override def setOnErrorListener(listener: OnErrorListener): Unit =
    onErrorListener = Option(listener)

  override def setOnPreparedListener(listener: OnPreparedListener): Unit =
    onPreparedListener = Option(listener)

  override def setOnSeekCompleteListener(listener: OnSeekCompleteListener): Unit =
    onSeekCompleteListener = Option(listener)

  override def setOnPlaybackSpeedAdjustmentListener(listener: OnAudioFxListener): Unit = ()

  override def setOnAsyncPositionUpdateListener(listener: OnAsyncPositionUpdateListener): Unit =
    onAsyncPositionUpdateListener = Option(listener)

  override def setOnPlaybackCapabilitiesChangedListener(listener: OnPlaybackCapabilitiesChangedListener): Unit =
    onPlaybackCapabilitiesChangedListener = Option(listener)

  override def setOnPlayerDisconnectedListener(listener: OnPlayerDisconnectedListener): Unit =
    onPlayerDisconnectedListener = Option(listener)

  override def setOnRemotePlayerStateChanged(listener: OnRemotePlayerStateChangedListener): Unit =
    onStateChangedListener = Option(listener)

  override def setOnRemoteEpisodeChangedListener(listener: OnRemoteEpisodeChangedListener): Unit =
    onEpisodeChangedListener = Option(listener)

  //
  // media status listener
  //

  override def onStatusUpdated(): Unit = {
    import MediaStatus._

    def playbackStateFor(status: MediaStatus): RemotePlaybackState.RemotePlaybackState = status.getPlayerState match {
      case PLAYER_STATE_IDLE => RemotePlaybackState.Idle
      case PLAYER_STATE_BUFFERING => RemotePlaybackState.Buffering
      case PLAYER_STATE_PAUSED => RemotePlaybackState.Paused
      case PLAYER_STATE_PLAYING => RemotePlaybackState.Playing
      case _ => RemotePlaybackState.Unknown
    }

    def mediaStatusToString(status: Option[MediaStatus]): String = status map { s =>
      val state = s.getPlayerState match {
        case PLAYER_STATE_IDLE =>
          val reason = s.getIdleReason match {
            case IDLE_REASON_NONE => "no reason"
            case IDLE_REASON_CANCELED => "canceled"
            case IDLE_REASON_INTERRUPTED => "interrupted"
            case IDLE_REASON_FINISHED => "finished"
            case IDLE_REASON_ERROR => "error"
          }
          s"idle ($reason)"
        case PLAYER_STATE_BUFFERING =>
          "buffering"
        case PLAYER_STATE_PAUSED =>
          "paused"
        case PLAYER_STATE_PLAYING =>
          "playing"
        case playerState =>
          s"unknown ($playerState)"
      }
      s"playerState=$state streamPosition=${s.getStreamPosition} playbackRate=${s.getPlaybackRate} streamVolume=${s.getStreamVolume}"
    } getOrElse "not available"

    def updatePositionIfApplicable(status: MediaStatus): Unit = {
      val playerState = status.getPlayerState
      if (playerState == MediaStatus.PLAYER_STATE_PAUSED || playerState == MediaStatus.PLAYER_STATE_PLAYING) {
        val streamPosition = status.getStreamPosition
        if (streamPosition > 0) {
          onAsyncPositionUpdateListener.foreach(_.onPositionUpdated(this, streamPosition.toInt))
        }
      }
    }

    def updatePlaybackCapabilities(status: MediaStatus): Unit = {
      if (status.isMediaCommandSupported(COMMAND_SEEK) != seekable) {
        seekable = !seekable
        onPlaybackCapabilitiesChangedListener.foreach(_.onPlaybackCapabilitiesChanged(this))
      }
    }

    def fireCompletionIfApplicable(status: MediaStatus): Boolean = {
      val finished = status.getPlayerState == PLAYER_STATE_IDLE && status.getIdleReason == IDLE_REASON_FINISHED
      val changed = finished && !completed
      if (changed) {
        log.info(s"remote player finished")
        onCompletionListener.foreach(_.onCompletion(this))
      }
      completed = finished
      changed
    }

    def fireEpisodeChangeIfApplicable(status: MediaStatus): Unit = {
      val newMediaUrl = Option(status.getMediaInfo).
        flatMap(mediaInfo => Option(mediaInfo.getContentId)).
        flatMap(contentId => Try(Some(new URL(contentId))).getOrElse(None))
      if (newMediaUrl != _currentMediaUrl) {
        log.info(s"new media url $newMediaUrl")
        _currentMediaUrl = newMediaUrl
        _currentMediaUrl foreach { mediaUrl => 
          onEpisodeChangedListener.foreach(_.onRemoteEpisodeChanged(mediaUrl, playbackStateFor(status)))
        }
      }
    }

    def fireStateChangeIfApplicable(status: MediaStatus): Unit = {
      val newState = playbackStateFor(status)
      if (newState != _currentState) {
        log.info(s"new playback state $newState")
        _currentState = newState
        newState match {
          case RemotePlaybackState.Buffering =>
            onStateChangedListener.foreach(_.onRemoteBuffering(this, _currentMediaUrl.get))
          case RemotePlaybackState.Playing =>
            onStateChangedListener.foreach(_.onRemotePlaying(this, _currentMediaUrl.get))
          case RemotePlaybackState.Paused =>
            onStateChangedListener.foreach(_.onRemotePaused(this, _currentMediaUrl.get))
          case RemotePlaybackState.Idle if status.getIdleReason == MediaStatus.IDLE_REASON_CANCELED =>
            onStateChangedListener.foreach(_.onRemoteStopped(this))
          case _ =>
        }
      }
    }

    if (!loading) {
      val mediaStatus = Option(mediaPlayer.getMediaStatus)
      log.debug(s"media status updated: ${mediaStatusToString(mediaStatus)}")
      mediaStatus foreach {
        status =>
          updatePositionIfApplicable(status)
          updatePlaybackCapabilities(status)
          if (!fireCompletionIfApplicable(status)) {
            fireEpisodeChangeIfApplicable(status)
            fireStateChangeIfApplicable(status)
          }
      }
    }
  }

  //
  // helpers
  //

  private def handleError(operation: String, status: Status): Unit = {
    loading = false
    status.getStatusCode match {
      case RemoteMediaPlayer.STATUS_REPLACED =>
        log.info(s"$operation has been replaced by a more recent request")
      case _ =>
        log.error(s"$operation failed: ${status.getStatusCode}")
        onErrorListener.foreach(_.onError(this, PlaybackError(PlaybackError.RemoteError, status.getStatusCode, 0)))
    }
  }
}