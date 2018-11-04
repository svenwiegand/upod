package mobi.upod.app.gui.episode.download

import android.content.Context
import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.gui.episode.{BulkEpisodeAction, BulkEpisodeUpdate}
import mobi.upod.app.services.download.DownloadService

private[download] abstract class RemoveEpisodesFromDownloadListAction(
    episodes: => IndexedSeq[EpisodeListItem])
    (implicit bindingModule: BindingModule)
  extends BulkEpisodeAction(episodes) with BulkEpisodeUpdate {
  protected lazy val downloadService = inject[DownloadService]

  protected def enabled(episodes: IndexedSeq[EpisodeListItem]) =
    downloadService.canRemoveAtLeastOneOf(episodes)

  protected def processData(context: Context, data: IndexedSeq[EpisodeListItem]) = {
    val removed = downloadService.removeEpisodes(data)
    removed.map(episode => episode.copy(downloadInfo = episode.downloadInfo.copy(listPosition = None)))
  }
}