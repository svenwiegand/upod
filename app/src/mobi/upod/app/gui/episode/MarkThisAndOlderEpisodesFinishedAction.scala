package mobi.upod.app.gui.episode

import android.content.Context
import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.services.playback.PlaybackService

class MarkThisAndOlderEpisodesFinishedAction(episode: => Option[EpisodeListItem])(implicit bindings: BindingModule)
  extends AsyncEpisodeAction(episode) {
  private lazy val playbackService = inject[PlaybackService]

  protected def enabled(episode: EpisodeListItem) =
    playbackService.canMarkFinished(episode)

  protected def processData(context: Context, data: EpisodeListItem) = {
    playbackService.markThisAndOlderEpisodesFinished(data)
    data.copy(
      library = false,
      playbackInfo = data.playbackInfo.copy(listPosition = None, finished = true),
      downloadInfo = data.downloadInfo.copy(listPosition = None))
  }
}
