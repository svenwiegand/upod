package mobi.upod.app.gui.episode

import android.content.Context
import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.services.playback.PlaybackService

abstract class MarkEpisodesUnfinishedAction(episodes: => IndexedSeq[EpisodeListItem])(implicit bindings: BindingModule)
  extends BulkEpisodeAction(episodes)
  with BulkEpisodeUpdate {
  private lazy val playbackService = inject[PlaybackService]

  protected def enabled(episodes: IndexedSeq[EpisodeListItem]): Boolean =
    playbackService.canMarkUnfinishedAtLeastOneOf(episodes)

  protected def processData(context: Context, data: IndexedSeq[EpisodeListItem]): Traversable[EpisodeListItem] = {
    val unfinished = playbackService.markEpisodesUnfinished(data)
    unfinished.map(e => e.copy(library = true, playbackInfo = e.playbackInfo.copy(finished = false)))
  }
}
