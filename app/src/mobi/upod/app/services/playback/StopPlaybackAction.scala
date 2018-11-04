package mobi.upod.app.services.playback

import com.escalatesoft.subcut.inject.BindingModule
import android.content.Context

class StopPlaybackAction(implicit bindings: BindingModule) extends PlaybackAction {

  protected def enabled = playbackService.canStop

  def onFired(context: Context) {
    playbackService.stop()
  }
}
