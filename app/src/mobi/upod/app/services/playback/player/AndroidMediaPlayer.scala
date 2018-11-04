package mobi.upod.app.services.playback.player

import android.graphics.SurfaceTexture
import android.media.{MediaPlayer => VPlayer}
import android.view.Surface
import mobi.upod.app.services.playback.VideoSize

private[player] final class AndroidMediaPlayer extends MediaPlayerImplementation {

  import MediaPlayer._

  private val player = new VPlayer
  private var _surface: Option[SurfaceTexture] = None
  private var careForSurface: Boolean = false

  protected def doReset() = player.reset()

  protected def doSetDataSource(path: String) = player.setDataSource(path)

  protected def doPrepareAsync() = player.prepareAsync()

  protected def currentPosition = player.getCurrentPosition

  protected def duration = player.getDuration

  protected def videoSize = VideoSize(player.getVideoWidth, player.getVideoHeight)

  protected def doPause() = player.pause()

  protected def doStart() = player.start()

  protected def doSeekTo(msec: Int, commit: Boolean) = {
    player.seekTo(msec)
    if (commit) {
      seekCompleteListener.foreach(_.onSeekComplete(this)) // The listener isn't called automatically -- a known APlayer problem
    }
  }

  protected def doStop() = player.stop()

  protected def surface = _surface

  protected def doSetSurface(surface: Option[SurfaceTexture]): Unit = {
    if (_surface != surface) {
      _surface = surface
      player.setSurface(surface.map(new Surface(_)).orNull)
    }
    careForSurface = false
  }

  protected def doSetCareForSurface(care: Boolean): Unit =
    careForSurface = care

  protected def getAudioFxAvailability = {
    if (!SonicMediaPlayer.isAvailable)
      AudioFxAvailability.NotSupported
    else if (mimeType.exists(_.isAudio))
      AudioFxAvailability.NotForCurrentPlayer
    else
      AudioFxAvailability.NotForCurrentDataSource
  }

  protected def doSetPlaybackSpeedMultiplier(multiplier: Float) =
    throw new UnsupportedOperationException

  protected def getPlaybackSpeedMultiplier = 1f

  override protected def doSetRelativeVolume(volume: Float): Unit =
    player.setVolume(volume, volume)

  override protected def doSetVolumeGain(gain: Float): Unit =
    throw new UnsupportedOperationException

  override protected def getVolumeGain = 1f

  protected def doRelease(): Unit = {
    player.release()
    if (careForSurface) {
      _surface.foreach(_.release())
    }
  }

  protected type CompletionListener = VPlayer.OnCompletionListener

  protected def wrappedCompletionListener(listener: OnCompletionListener) = new CompletionListener {
    def onCompletion(mp: VPlayer): Unit = listener.onCompletion(AndroidMediaPlayer.this)
  }

  protected def setCompletionListener(wrappedListener: CompletionListener) =
    player.setOnCompletionListener(wrappedListener)

  protected type ErrorListener = VPlayer.OnErrorListener

  protected def wrappedErrorListener(listener: OnErrorListener) = new ErrorListener {
    def onError(mp: VPlayer, what: Int, extra: Int) =
      listener.onError(AndroidMediaPlayer.this, createPlaybackError(what, extra))
  }

  protected def setErrorListener(wrappedListener: ErrorListener) =
    player.setOnErrorListener(wrappedListener)

  protected type PreparedListener = VPlayer.OnPreparedListener

  protected def wrappedPreparedListener(listener: OnPreparedListener) = new PreparedListener {
    def onPrepared(mp: VPlayer) = listener.onPrepared(AndroidMediaPlayer.this)
  }

  protected def setPreparedListener(wrappedListener: AndroidMediaPlayer#PreparedListener) =
    player.setOnPreparedListener(wrappedListener)

  protected type SeekCompleteListener = VPlayer.OnSeekCompleteListener

  protected def wrappedSeekCompleteListener(listener: OnSeekCompleteListener) = new SeekCompleteListener {
    def onSeekComplete(mp: VPlayer) = listener.onSeekComplete(AndroidMediaPlayer.this)
  }

  protected def setSeekCompleteListener(wrappedListener: AndroidMediaPlayer#SeekCompleteListener) =
    player.setOnSeekCompleteListener(wrappedListener)

  def setOnPlaybackSpeedAdjustmentListener(listener: OnAudioFxListener): Unit = ()
}
