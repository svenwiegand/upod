package mobi.upod.app.services.playback.player

import java.io.IOException

import android.graphics.SurfaceTexture
import mobi.upod.android.logging.Logging
import mobi.upod.app.data.{EpisodeListItem, MimeType}
import mobi.upod.app.services.playback.{PlaybackError, VideoSize}
import mobi.upod.app.storage.StorageProvider

private[player] trait MediaPlayerImplementation extends MediaPlayer with Logging {

  import MediaPlayer._

  private var _mimeType: Option[MimeType] = None
  protected var seekCompleteListener: Option[OnSeekCompleteListener] = None
  protected var errorListener: Option[OnErrorListener] = None

  protected def mimeType: Option[MimeType] = _mimeType

  protected def logCall(call: => String): Unit =
    log.debug(call)

  def reset(): Unit = {
    logCall("reset")
    doReset()
  }

  protected def doReset(): Unit

  override val playsLocalFiles: Boolean = true

  def load(storageProvider: StorageProvider, episode: EpisodeListItem, msec: Int): Unit = {
    if (storageProvider.readable) {
      val path = episode.mediaFile(storageProvider).getAbsolutePath
      logCall(s"setDataSource($path)")
      doSetDataSource(path)
      _mimeType = Some(episode.media.mimeType)
      doPrepareAsync()
    } else {
      throw new IOException("storage provider not readable")
    }
  }

  protected def doSetDataSource(path: String): Unit

  protected def doPrepareAsync(): Unit

  def getCurrentPosition: Int = {
    val pos = currentPosition
    logCall(s"getCurrentPosition $pos")
    pos
  }

  protected def currentPosition: Int

  def getDuration: Int = {
    logCall("getDuration")
    duration
  }

  protected def duration: Int

  def getVideoSize: VideoSize = {
    logCall("getVideoSize")
    videoSize
  }

  protected def videoSize: VideoSize

  def pause(): Unit = {
    logCall("pause")
    doPause()
  }

  protected def doPause(): Unit

  def start(): Unit = {
    logCall(s"start at $currentPosition")
    doStart()
  }

  protected def doStart(): Unit

  override val canSeek: Boolean = true

  def seekTo(msec: Int, commit: Boolean): Unit = {
    logCall(s"seekTo $msec")
    doSeekTo(msec, commit)
  }

  protected def doSeekTo(msec: Int, commit: Boolean): Unit

  def stop(): Unit = {
    logCall("stop")
    doStop()
  }

  protected def doStop(): Unit

  def getSurface: Option[SurfaceTexture] = {
    logCall("getSurface")
    surface
  }

  protected def surface: Option[SurfaceTexture]

  def setSurface(surface: Option[SurfaceTexture]): Unit = {
    logCall("setSurfaceTexture")
    doSetSurface(surface)
  }

  protected def doSetSurface(surface: Option[SurfaceTexture]): Unit

  def setCareForSurface(care: Boolean): Unit = {
    logCall(s"setCareForSurface($care)")
    doSetCareForSurface(care)
  }

  protected def doSetCareForSurface(care: Boolean): Unit

  def audioFxAvailability = {
    logCall("playbackSpeedAdjustmentAvailability")
    getAudioFxAvailability
  }

  protected def getAudioFxAvailability: AudioFxAvailability.AudioFxAvailability

  def setPlaybackSpeedMultiplier(multiplier: Float): Unit = {
    logCall(s"setPlaybackSpeedMultiplier($multiplier)")
    doSetPlaybackSpeedMultiplier(multiplier)
  }

  protected def doSetPlaybackSpeedMultiplier(multiplier: Float): Unit

  def playbackSpeedMultiplier: Float = {
    logCall("playbackSpeedMultiplier")
    getPlaybackSpeedMultiplier
  }

  protected def getPlaybackSpeedMultiplier: Float

  override def setRelativeVolume(volume: Float): Unit = {
    logCall(s"setRelativeVolume($volume)")
    doSetRelativeVolume(volume)
  }

  protected def doSetRelativeVolume(volume: Float): Unit

  override def setVolumeGain(gain: Float): Unit = {
    logCall(s"setVolumeGain($gain)")
    doSetVolumeGain(gain)
  }

  protected def doSetVolumeGain(gain: Float): Unit

  override def volumeGain: Float = {
    logCall("volumeGain")
    getVolumeGain
  }

  protected def getVolumeGain: Float

  def release(): Unit = {
    logCall("release")

    doRelease()
  }

  protected def doRelease(): Unit

  //
  // listeners
  //

  private def setListener[A, B](name: String, listener: A, wrappedListener: A => B, set: B => Unit): Unit = {
    logCall(s"setOn${name}Listener")
    val l: B = if (listener == null) null.asInstanceOf[B] else wrappedListener(listener)
    set(l)
  }

  protected type CompletionListener

  def setOnCompletionListener(listener: OnCompletionListener): Unit =
    setListener("Completion", listener, wrappedCompletionListener, setCompletionListener)
  
  protected def wrappedCompletionListener(listener: OnCompletionListener): CompletionListener

  protected def setCompletionListener(wrappedListener: CompletionListener): Unit

  protected type ErrorListener
  
  def setOnErrorListener(listener: OnErrorListener): Unit = {
    errorListener = Option(listener)
    setListener("Error", listener, wrappedErrorListener, setErrorListener)
  }

  protected def createPlaybackError(what: Int, extra: Int): PlaybackError =
    PlaybackError(PlaybackError.Unknown, what, extra)

  protected def wrappedErrorListener(listener: OnErrorListener): ErrorListener

  protected def setErrorListener(wrappedListener: ErrorListener): Unit

  protected type PreparedListener
  
  def setOnPreparedListener(listener: OnPreparedListener): Unit = 
    setListener("Prepared", listener, wrappedPreparedListener, setPreparedListener)
  
  protected def wrappedPreparedListener(listener: OnPreparedListener): PreparedListener
  
  protected def setPreparedListener(wrappedListener: PreparedListener): Unit

  protected type SeekCompleteListener
  
  def setOnSeekCompleteListener(listener: OnSeekCompleteListener): Unit = {
    seekCompleteListener = Option(listener)
    setListener("SeekComplete", listener, wrappedSeekCompleteListener, setSeekCompleteListener)
  }

  protected def wrappedSeekCompleteListener(listener: OnSeekCompleteListener): SeekCompleteListener
  
  protected def setSeekCompleteListener(wrappedListener: SeekCompleteListener): Unit

  override def setOnAsyncPositionUpdateListener(listener: OnAsyncPositionUpdateListener): Unit = ()

  override def setOnPlaybackCapabilitiesChangedListener(listener: OnPlaybackCapabilitiesChangedListener): Unit = ()

  override def setOnPlayerDisconnectedListener(listener: OnPlayerDisconnectedListener) = ()

  override def setOnRemotePlayerStateChanged(listener: OnRemotePlayerStateChangedListener): Unit = ()

  override def setOnRemoteEpisodeChangedListener(listener: OnRemoteEpisodeChangedListener): Unit = ()
}
