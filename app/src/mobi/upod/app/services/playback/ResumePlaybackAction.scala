package mobi.upod.app.services.playback

import android.content.Context
import com.escalatesoft.subcut.inject.BindingModule

class ResumePlaybackAction(alwaysEnabled: Boolean = false)(implicit bindings: BindingModule) extends PlaybackAction {
  protected def enabled = alwaysEnabled || playbackService.canResume

  def onFired(context: Context): Unit = {
    playbackService.resume(Some(context))
  }
}
