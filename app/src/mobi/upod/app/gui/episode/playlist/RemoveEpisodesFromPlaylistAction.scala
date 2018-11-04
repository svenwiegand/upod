package mobi.upod.app.gui.episode.playlist

import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.gui.episode.{BulkEpisodeAction, BulkEpisodeUpdate}
import mobi.upod.app.services.playback.PlaybackService
import android.content.Context

private[playlist] abstract class RemoveEpisodesFromPlaylistAction(
    episodes: => IndexedSeq[EpisodeListItem])
    (implicit bindingModule: BindingModule)
  extends BulkEpisodeAction(episodes) with BulkEpisodeUpdate {
  protected lazy val playService = inject[PlaybackService]

  protected def enabled(episodes: IndexedSeq[EpisodeListItem]) =
    playService.canRemoveAtLeastOneOf(episodes)

  protected def processData(context: Context, data: IndexedSeq[EpisodeListItem]) = {
    val removed = playService.removeEpisodes(data)
    removed.map(episode => episode.copy(playbackInfo = episode.playbackInfo.copy(listPosition = None)))
  }
}