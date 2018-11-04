package mobi.upod.app.gui.episode.playlist

import android.view.View
import com.escalatesoft.subcut.inject.BindingModule
import mobi.upod.android.app.action.Action
import mobi.upod.app.R
import mobi.upod.app.gui.episode._
import mobi.upod.app.gui.episode.download._
import mobi.upod.app.gui.episode.library.StarEpisodeAction
import mobi.upod.app.gui.episode.EpisodeListItemViewHolderConfiguration
import mobi.upod.app.gui.episode.news.AddEpisodeToLibraryAction

class PlaylistEpisodeListItemViewHolder
  (view: View, config: EpisodeListItemViewHolderConfiguration)
  (implicit bindingModule: BindingModule)
  extends EpisodeListItemViewHolder(view, config)
  with EpisodeOrderActions {

  protected def contextMenuResourceId = R.menu.playlist_episode_context

  protected override def createActions: Map[Int, Action] = super.createActions ++ Map(
    R.id.action_mark_read -> new AddEpisodeToLibraryAction(episodeListItem) with EpisodeAdapterUpdate,
    R.id.action_download_error -> new EpisodeDownloadErrorAction(episodeListItem),
    R.id.action_download -> new DownloadEpisodeAction(episodeListItem),
    R.id.action_stop_download -> new StopEpisodeDownloadAction(episodeListItem),
    R.id.action_add_download -> new AddEpisodeToDownloadListAction(episodeListItem) with EpisodeAdapterUpdate,
    R.id.action_delete_download -> new DeleteEpisodeDownloadAction(episodeListItem) with EpisodeAdapterUpdate,
    R.id.action_pause -> new PauseEpisodeAction(episodeListItem),
    R.id.action_cast -> new CastEpisodeAction(episodeListItem),
    R.id.action_stream -> new StreamEpisodeAction(episodeListItem),
    R.id.action_play -> new PlayEpisodeAction(episodeListItem),
    R.id.action_star -> new StarEpisodeAction(episodeListItem, true) with EpisodeAdapterUpdate,
    R.id.action_unstar -> new StarEpisodeAction(episodeListItem, false) with EpisodeAdapterUpdate,
    R.id.action_mark_unfinished -> new MarkEpisodeUnfinishedAction(episodeListItem) with EpisodeAdapterUpdate,
    R.id.action_mark_finished -> new MarkEpisodeFinishedAction(episodeListItem) with ViewDismissAction with DismissActionInfo,
    R.id.action_remove_from_playlist -> new RemoveEpisodeFromPlaylistAction(episodeListItem) with EpisodeAdapterUpdate with ViewDismissAction with DismissActionInfo
  )

  override protected def shouldShowDragHandle: Boolean =
    config.enableDragHandle && !isPlaying
}
