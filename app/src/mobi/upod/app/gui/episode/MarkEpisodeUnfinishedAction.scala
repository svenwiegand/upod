package mobi.upod.app.gui.episode

import android.content.Context
import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.services.playback.PlaybackService

class MarkEpisodeUnfinishedAction(episode: => Option[EpisodeListItem])(implicit bindings: BindingModule)
  extends AsyncEpisodeAction(episode) {
  private lazy val playbackService = inject[PlaybackService]

  protected def enabled(episode: EpisodeListItem) =
    episode.playbackInfo.finished

  protected def processData(context: Context, data: EpisodeListItem) = {
    playbackService.markEpisodeUnfinished(data)
    data.copy(library = true, playbackInfo = data.playbackInfo.copy(finished = false))
  }
}
