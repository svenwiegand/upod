package mobi.upod.app.gui.episode.download

import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.gui.episode.AsyncEpisodeAction
import mobi.upod.app.services.download.DownloadService
import android.content.Context

private[episode] class StopEpisodeDownloadAction(episode: => Option[EpisodeListItem])(implicit bindings: BindingModule)
  extends AsyncEpisodeAction(episode){

  private lazy val downloadService = inject[DownloadService]

  protected def enabled(episode: EpisodeListItem) =
    downloadService.immediateDownloadQueue.exists(_.id == episode.id)

  protected def processData(context: Context, data: EpisodeListItem) = {
    downloadService.stopDownload()
    data
  }
}
