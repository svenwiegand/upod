package mobi.upod.app.gui.episode.library

import mobi.upod.app.data.EpisodeListItem
import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.gui.episode.{EpisodeUpdate, AsyncEpisodeAction}
import mobi.upod.app.services.EpisodeService
import android.content.Context

abstract class StarEpisodeAction(episode: => Option[EpisodeListItem], star: Boolean)(implicit bindings: BindingModule)
  extends AsyncEpisodeAction(episode) with EpisodeUpdate {

  private lazy val episodeService = inject[EpisodeService]

  protected def enabled(episode: EpisodeListItem): Boolean =
    episodeService.canChangeStar(episode, star)

  protected def processData(context: Context, data: EpisodeListItem): EpisodeListItem = {
    episodeService.starEpisodes(Traversable(data), star)
    data.copy(starred = star)
  }
}
