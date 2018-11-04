package mobi.upod.app.gui.episode.download

import com.escalatesoft.subcut.inject.BindingModule
import com.github.nscala_time.time.Imports._
import mobi.upod.app.gui.episode.{BulkEpisodeUpdate, BulkEpisodeAction}
import mobi.upod.app.storage.EpisodeDao
import mobi.upod.app.services.download.DownloadService
import mobi.upod.app.data.EpisodeListItem
import android.content.Context

private[episode] abstract class AddEpisodesToDownloadListAction(
    episodes: => IndexedSeq[EpisodeListItem])
    (implicit bindingModule: BindingModule)
  extends BulkEpisodeAction(episodes) with BulkEpisodeUpdate {
  private val downloadService = inject[DownloadService]
  private val episodeDao = inject[EpisodeDao]

  protected def enabled(episodes: IndexedSeq[EpisodeListItem]) =
    episodes.exists(episode => !episode.downloadInfo.complete && episode.downloadInfo.listPosition.isEmpty)

  protected def processData(context: Context, data: IndexedSeq[EpisodeListItem]) = {
    val ids = data.sortBy(_.published).map(_.id)
    downloadService.addDownloads(ids)
    episodeDao.findListItemsByIds(ids).toSeqAndClose()
  }
}
