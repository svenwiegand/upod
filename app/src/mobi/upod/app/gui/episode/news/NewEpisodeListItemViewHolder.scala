package mobi.upod.app.gui.episode.news

import android.view.View
import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.android.app.action.Action
import mobi.upod.app.R
import mobi.upod.app.gui.episode.{EpisodeListItemViewHolderConfiguration, _}
import mobi.upod.app.gui.episode.download._
import mobi.upod.app.gui.episode.library.StarEpisodeAction
import mobi.upod.app.gui.episode.playlist._

class NewEpisodeListItemViewHolder
  (view: View, config: EpisodeListItemViewHolderConfiguration)
  (implicit bindingModule: BindingModule)
  extends EpisodeListItemViewHolder(view, config) {

  protected def contextMenuResourceId = R.menu.new_episode_context

  protected val createActions: Map[Int, Action] = Map(
    R.id.action_mark_read -> new AddEpisodeToLibraryAction(episodeListItem) with ViewDismissAction with DismissActionInfo,
    R.id.action_download_error -> new EpisodeDownloadErrorAction(episodeListItem),
    R.id.action_download -> new DownloadEpisodeAction(episodeListItem) with ViewDismissAction with DismissActionInfo,
    R.id.action_stop_download -> new StopEpisodeDownloadAction(episodeListItem),
    R.id.action_add_download -> new AddEpisodeToDownloadListAction(episodeListItem) with EpisodeAdapterUpdate with ViewDismissAction with DismissActionInfo,
    R.id.action_delete_download -> new DeleteEpisodeDownloadAction(episodeListItem) with EpisodeAdapterUpdate,
    R.id.action_pause -> new PauseEpisodeAction(episodeListItem),
    R.id.action_cast -> new CastEpisodeAction(episodeListItem) with ViewDismissAction with DismissActionInfo,
    R.id.action_stream -> new StreamEpisodeAction(episodeListItem) with ViewDismissAction with DismissActionInfo,
    R.id.action_play -> new PlayEpisodeAction(episodeListItem) with ViewDismissAction with DismissActionInfo,
    R.id.action_play_next -> new PlayEpisodeNextAction(episodeListItem) with EpisodeAdapterUpdate with ViewDismissAction with DismissActionInfo,
    R.id.action_add_to_playlist -> new AddEpisodeToPlaylistAction(episodeListItem) with EpisodeAdapterUpdate with ViewDismissAction with DismissActionInfo,
    R.id.action_star -> new StarEpisodeAction(episodeListItem, true) with EpisodeAdapterUpdate with ViewDismissAction with DismissActionInfo,
    R.id.action_unstar -> new StarEpisodeAction(episodeListItem, false) with EpisodeAdapterUpdate,
    R.id.action_mark_unfinished -> new MarkEpisodeUnfinishedAction(episodeListItem) with ViewDismissAction with DismissActionInfo,
    R.id.action_mark_finished -> new MarkEpisodeFinishedAction(episodeListItem) with ViewDismissAction with DismissActionInfo,
    R.id.action_mark_finished_from_here -> new MarkEpisodesFinishedFromHereAction,
    R.id.action_mark_finished_to_here -> new MarkEpisodesFinishedToHereAction
  )

}
