package mobi.upod.app.gui.episode.download

import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.gui.episode.AsyncEpisodeAction
import mobi.upod.app.services.download.DownloadService
import android.content.Context
import mobi.upod.app.services.EpisodeService

private[episode] class DeleteEpisodeDownloadAction(episode: => Option[EpisodeListItem])(implicit bindings: BindingModule)
  extends AsyncEpisodeAction(episode) {
  private lazy val downloadService = inject[DownloadService]
  private lazy val episodeService = inject[EpisodeService]

  protected def enabled(episode: EpisodeListItem) =
    episode.downloadInfo.fetchedBytes > 0 && episodeService.canDeleteAtLeastOneDownload(Traversable(episode))

  protected def processData(context: Context, data: EpisodeListItem) = {
    downloadService.deleteDownload(data)
    data.copy(downloadInfo = data.downloadInfo.copy(fetchedBytes = 0, complete = false))
  }
}

