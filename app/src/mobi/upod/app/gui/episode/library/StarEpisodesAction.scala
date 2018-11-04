package mobi.upod.app.gui.episode.library

import android.content.Context
import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.gui.episode.{BulkEpisodeUpdate, BulkEpisodeAction}

private[episode] abstract class StarEpisodesAction(episodes: => IndexedSeq[EpisodeListItem], star: Boolean)(implicit bindings: BindingModule)
  extends BulkEpisodeAction(episodes)
  with BulkEpisodeUpdate {

  protected def enabled(episodes: IndexedSeq[EpisodeListItem]): Boolean =
    episodeService.canChangeStarFor(episodes, star)

  protected def processData(context: Context, data: IndexedSeq[EpisodeListItem]): Traversable[EpisodeListItem] =
    episodeService.starEpisodes(data, star).map(_.copy(starred = star))
}
