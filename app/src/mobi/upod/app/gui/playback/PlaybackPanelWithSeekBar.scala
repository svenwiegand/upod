package mobi.upod.app.gui.playback

import android.widget.SeekBar
import mobi.upod.android.widget.TintableSeekBar
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.util.Permille.LongPermille

trait PlaybackPanelWithSeekBar extends PlaybackPanel with SeekBar.OnSeekBarChangeListener {
  protected lazy val seekBar = mediaProgressView.asInstanceOf[TintableSeekBar]
  private var _seeking = false

  protected def seeking = _seeking

  override protected def createPlaybackPanel(): Unit = {
    seekBar.setOnSeekBarChangeListener(this)
    super.createPlaybackPanel()
  }

  override protected def positionUpdateAllowed: Boolean = !_seeking

  override def invalidateActionButtons(): Unit = {
    seekBar.setEnabled(playbackService.canSeek)
    super.invalidateActionButtons()
  }

  override def onStartTrackingTouch(seekBar: SeekBar): Unit =
    _seeking = true

  override def onStopTrackingTouch(seekBar: SeekBar): Unit = {
    episode.foreach { e =>
      playbackService.seek(seekBar.getProgress.toLong.fromPermille(e.media.duration), true)
    }
    if (playbackService.isPlaying)
      seekBar.postDelayed(1000l, _seeking = false )
    else
      _seeking = false
  }

  override def onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean): Unit = if (fromUser) {
    episode.foreach { e =>
      val position = progress.toLong.fromPermille(e.media.duration)
      updatePlaybackPosition(position, e.media.duration)
      playbackService.seek(position, false)
    }
  }
}