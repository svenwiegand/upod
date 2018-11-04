package mobi.upod.app.gui.episode.news

import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.gui.episode.{BulkEpisodeUpdate, BulkEpisodeAction}
import mobi.upod.app.data.EpisodeListItem
import android.content.Context

private[episode] abstract class AddEpisodesToLibraryAction(
    episodes: => IndexedSeq[EpisodeListItem])
    (implicit bindingModule: BindingModule)
  extends BulkEpisodeAction(episodes) with BulkEpisodeUpdate {

  protected def enabled(episodes: IndexedSeq[EpisodeListItem]) =
    episodes.exists(e => !e.library && !e.playbackInfo.finished)

  protected def processData(context: Context, data: IndexedSeq[EpisodeListItem]) = {
    episodeService.addToLibrary(data.map(_.id))
    data.map(_.copy(library = true))
  }
}
