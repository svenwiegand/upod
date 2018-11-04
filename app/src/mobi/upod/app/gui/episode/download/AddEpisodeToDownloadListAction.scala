package mobi.upod.app.gui.episode.download

import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.gui.episode.{EpisodeUpdate, AsyncEpisodeAction}
import mobi.upod.app.storage.EpisodeDao
import mobi.upod.app.services.download.DownloadService
import mobi.upod.app.data.EpisodeListItem
import android.content.Context

private[episode] abstract class AddEpisodeToDownloadListAction(episode: => Option[EpisodeListItem])(implicit bindings: BindingModule)
  extends AsyncEpisodeAction(episode) with EpisodeUpdate {
  private val downloadService = inject[DownloadService]
  private val episodeDao = inject[EpisodeDao]

  protected def enabled(episode: EpisodeListItem) =
    !episode.downloadInfo.complete && episode.downloadInfo.listPosition.isEmpty

  protected def processData(context: Context, data: EpisodeListItem) = {
    downloadService.addDownloads(Traversable(data.id))
    episodeDao.findListItemById(data.id).getOrElse(data)
  }
}
