package mobi.upod.app.gui.episode.playlist

import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.gui.episode.{EpisodeUpdate, AsyncEpisodeAction}
import mobi.upod.app.services.playback.PlaybackService
import mobi.upod.app.storage.EpisodeDao
import android.content.Context

private[episode] abstract class AddEpisodeToPlaylistAction(episode: => Option[EpisodeListItem])(implicit bindings: BindingModule)
  extends AsyncEpisodeAction(episode) with EpisodeUpdate {
  private lazy val playService = inject[PlaybackService]
  private lazy val episodeDao = inject[EpisodeDao]

  protected def enabled(episode: EpisodeListItem) =
    episode.playbackInfo.listPosition.isEmpty

  protected def processData(context: Context, data: EpisodeListItem) = {
    playService.addEpisodes(Traversable(data.id))
    episodeDao.findListItemById(data.id).getOrElse(data)
  }
}
