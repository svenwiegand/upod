package mobi.upod.app.gui.episode.download

import android.content.Context
import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.gui.episode.{EpisodeUpdate, AsyncEpisodeAction}
import mobi.upod.app.services.download.DownloadService

private[episode] abstract class RemoveEpisodeFromDownloadListAction(episode: => Option[EpisodeListItem])(implicit bindings: BindingModule)
  extends AsyncEpisodeAction(episode) with EpisodeUpdate {

  private lazy val downloadService = inject[DownloadService]

  protected def enabled(episode: EpisodeListItem) =
    downloadService.canRemoveAtLeastOneOf(Traversable(episode))

  protected def processData(context: Context, data: EpisodeListItem) = {
    val removed = downloadService.removeEpisodes(Traversable(data))
    if (removed.isEmpty)
      data
    else
      data.copy(downloadInfo = data.downloadInfo.copy(listPosition = None))
  }
}
