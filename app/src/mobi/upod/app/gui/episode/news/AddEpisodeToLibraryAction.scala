package mobi.upod.app.gui.episode.news

import mobi.upod.app.gui.episode.AsyncEpisodeAction
import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.services.EpisodeService
import mobi.upod.app.data.EpisodeListItem
import android.content.Context

private[episode] class AddEpisodeToLibraryAction(episode: => Option[EpisodeListItem])(implicit bindings: BindingModule)
  extends AsyncEpisodeAction(episode) {
  private lazy val episodeService = inject[EpisodeService]

  protected def enabled(episode: EpisodeListItem) =
    !episode.library && !episode.playbackInfo.finished

  protected def processData(context: Context, data: EpisodeListItem) = {
    episodeService.addToLibrary(Traversable(data.id))
    data.copy(library = true)
  }
}
