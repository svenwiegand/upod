package mobi.upod.app.services.playback

import android.content.Context
import com.escalatesoft.subcut.inject.BindingModule

class PausePlaybackAction(implicit bindings: BindingModule) extends PlaybackAction {

  protected def enabled = playbackService.canPause

  def onFired(context: Context) {
    playbackService.pause()
  }
}
