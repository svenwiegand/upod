package mobi.upod.app.gui.episode.playlist

import com.escalatesoft.subcut.inject.BindingModule
import com.github.nscala_time.time.Imports._
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.gui.episode.{BulkEpisodeUpdate, BulkEpisodeAction}
import mobi.upod.app.services.playback.PlaybackService
import mobi.upod.app.storage.EpisodeDao
import android.content.Context

private[episode] abstract class AddEpisodesToPlaylistAction(
    episodes: => IndexedSeq[EpisodeListItem])
    (implicit bindingModule: BindingModule)
  extends BulkEpisodeAction(episodes) with BulkEpisodeUpdate {
  protected lazy val playService = inject[PlaybackService]
  private val episodeDao = inject[EpisodeDao]

  protected def enabled(episodes: IndexedSeq[EpisodeListItem]) =
    episodes.exists(_.playbackInfo.listPosition.isEmpty)

  protected def processData(context: Context, data: IndexedSeq[EpisodeListItem]) = {
    val ids = data.sortBy(_.published).map(_.id)
    playService.addEpisodes(ids)
    episodeDao.findListItemsByIds(ids).toSeqAndClose()
  }
}
