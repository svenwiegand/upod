package mobi.upod.app.services.playback

import android.content.Context
import com.escalatesoft.subcut.inject.BindingModule

class SkipEpisodePlaybackAction(implicit bindings: BindingModule) extends PlaybackAction {

  protected def enabled = playbackService.canSeek

  def onFired(context: Context) {
    playbackService.skip()
  }
}
