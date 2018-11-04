package mobi.upod.app.services.playback.player

import android.content.Context
import android.graphics.SurfaceTexture
import com.falconware.prestissimo.SonicPlayer
import mobi.upod.android.util.ApiLevel
import mobi.upod.app.services.playback.VideoSize

private[player] final class SonicMediaPlayer(context: Context)
  extends MediaPlayerImplementation {

  import MediaPlayer._

  private val player = new SonicPlayer(context)

  private var audioFxListener: Option[OnAudioFxListener] = None

  protected def doReset() = {
    player.reset()
    audioFxListener.foreach(_.onAudioFxAvailable(false))
  }

  protected def doSetDataSource(path: String) = player.setDataSource(path)

  protected def doPrepareAsync() = player.prepareAsync()

  protected def currentPosition = player.getCurrentPosition

  protected def duration = player.getDuration

  protected def videoSize = VideoSize(0, 0)

  protected def doPause() = player.pause()

  protected def doStart() = player.start()

  protected def doSeekTo(msec: Int, commit: Boolean) = player.seekTo(msec)

  protected def doStop(): Unit = {
    player.stop()
    audioFxListener.foreach(_.onAudioFxAvailable(false))
  }

  protected def surface = None

  protected def doSetSurface(surface: Option[SurfaceTexture]) = ()

  protected def doSetCareForSurface(care: Boolean) = ()

  protected def getAudioFxAvailability =
    AudioFxAvailability.Available

  protected def doSetPlaybackSpeedMultiplier(multiplier: Float) = {
    player.setPlaybackSpeed(multiplier)
    audioFxListener.foreach(_.onPlaybackSpeedChange(multiplier))
  }

  protected def getPlaybackSpeedMultiplier =
    player.getCurrentSpeed

  override protected def doSetRelativeVolume(volume: Float): Unit =
    player.setVolume(volume)

  override protected def doSetVolumeGain(gain: Float): Unit = {
    player.setVolumeGain(gain)
    audioFxListener.foreach(_.onVolumeGainChange(gain))
  }

  override protected def getVolumeGain: Float =
    player.getVolumeGain

  protected def doRelease() = player.release()

  protected type CompletionListener = SonicPlayer.OnCompletionListener

  protected def wrappedCompletionListener(listener: OnCompletionListener) = new CompletionListener {
    def onCompletion(): Unit = listener.onCompletion(SonicMediaPlayer.this)
  }

  protected def setCompletionListener(wrappedListener: CompletionListener) =
    player.setOnCompletionListener(wrappedListener)

  protected type ErrorListener = SonicPlayer.OnErrorListener

  protected def wrappedErrorListener(listener: OnErrorListener) = new ErrorListener {
    def onError(what: Int, extra: Int) =
      listener.onError(SonicMediaPlayer.this, createPlaybackError(what, extra))
  }

  protected def setErrorListener(wrappedListener: ErrorListener) =
    player.setOnErrorListener(wrappedListener)

  protected type PreparedListener = SonicPlayer.OnPreparedListener

  protected def wrappedPreparedListener(listener: OnPreparedListener) = new PreparedListener {
    def onPrepared() = listener.onPrepared(SonicMediaPlayer.this)
  }

  protected def setPreparedListener(wrappedListener: SonicMediaPlayer#PreparedListener) =
    player.setOnPreparedListener(wrappedListener)

  protected type SeekCompleteListener = SonicPlayer.OnSeekCompleteListener

  protected def wrappedSeekCompleteListener(listener: OnSeekCompleteListener) = new SeekCompleteListener {
    def onSeekComplete() = listener.onSeekComplete(SonicMediaPlayer.this)
  }

  protected def setSeekCompleteListener(wrappedListener: SonicMediaPlayer#SeekCompleteListener) =
    player.setOnSeekCompleteListener(wrappedListener)

  def setOnPlaybackSpeedAdjustmentListener(listener: OnAudioFxListener): Unit = {
    audioFxListener = Option(listener)
    audioFxListener.foreach(_.onAudioFxAvailable(true))
  }
}

object SonicMediaPlayer {

  val isAvailable: Boolean = ApiLevel >= ApiLevel.JellyBean
}