package mobi.upod.app.gui.episode

import android.content.Context
import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.services.playback.PlaybackService

abstract class MarkEpisodesFinishedAction(episodes: => IndexedSeq[EpisodeListItem])(implicit bindings: BindingModule)
  extends BulkEpisodeAction(episodes)
  with BulkEpisodeUpdate {
  private lazy val playbackService = inject[PlaybackService]

  protected def enabled(episodes: IndexedSeq[EpisodeListItem]): Boolean =
    playbackService.canMarkFinishedAtLeastOneOf(episodes)

  protected def processData(context: Context, data: IndexedSeq[EpisodeListItem]): Traversable[EpisodeListItem] = {
    val finished = playbackService.markEpisodesFinished(data)
    finished.map(e => e.copy(
      library = false,
      playbackInfo = e.playbackInfo.copy(listPosition = None, finished = true),
      downloadInfo = e.downloadInfo.copy(listPosition = None)))
  }
}
