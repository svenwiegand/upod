package mobi.upod.app.gui.episode.library

import mobi.upod.android.app.action.{SimpleFragmentActions, Action}
import mobi.upod.app.R
import mobi.upod.app.gui.episode._
import mobi.upod.app.gui.episode.playlist.AddEpisodesToPlaylistAction
import mobi.upod.app.gui.episode.download.{DeleteRecentlyFinishedEpisodesAction, AddEpisodesToDownloadListAction}
import mobi.upod.app.gui.ReloadOnEpisodeListChangedFragment
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.gui.episode.news.AddEpisodesToLibraryAction
import android.view.View

private[library] abstract class LibraryEpisodeListFragmentBase
  extends EpisodeListFragment
  with ReloadOnEpisodeListChangedFragment[IndexedSeq[EpisodeListItem]] {

  protected val contextualMenuResourceId = R.menu.library_episode_contextual
  protected override def createActions: Map[Int, Action] = super.createActions ++ Map(
    R.id.action_add_download -> new AddEpisodesToDownloadListAction(checkedEpisodes) with BulkEpisodeAdapterUpdate,
    R.id.action_add_to_playlist -> new AddEpisodesToPlaylistAction(checkedEpisodes) with BulkEpisodeAdapterUpdate,
    R.id.action_star -> new StarEpisodesAction(checkedEpisodes, true) with BulkEpisodeAdapterUpdate,
    R.id.action_unstar -> new StarEpisodesAction(checkedEpisodes, false) with BulkEpisodeAdapterUpdate,
    R.id.action_mark_finished -> new MarkEpisodesFinishedAction(checkedEpisodes) with BulkEpisodeAdapterUpdate with BulkViewDismissAction with DismissActionInfo,
    R.id.action_mark_unfinished -> new MarkEpisodesUnfinishedAction(checkedEpisodes) with BulkEpisodeAdapterUpdate,
    R.id.action_mark_read -> new AddEpisodesToLibraryAction(checkedEpisodes) with BulkEpisodeAdapterUpdate
  )

  protected def viewHolder(config: EpisodeListItemViewHolderConfiguration): View => EpisodeListItemViewHolder =
    new LibraryEpisodeListItemViewHolder(_, config)
}

private[library] trait FinishedFragmentActions extends LibraryEpisodeListFragmentBase with SimpleFragmentActions {

  override protected val optionsMenuResourceId = R.menu.finished_episode_actions

  override protected def createActions: Map[Int, Action] = super.createActions ++ Map(
    R.id.action_delete_recently_finished -> new DeleteRecentlyFinishedEpisodesAction(optionalAdapter.exists(_.episodes.nonEmpty)) with ActionReload,
    R.id.action_mark_finished -> new MarkEpisodesFinishedAction(checkedEpisodes) with BulkEpisodeAdapterUpdate,
    R.id.action_mark_unfinished -> new MarkEpisodesUnfinishedAction(checkedEpisodes) with BulkEpisodeAdapterUpdate with BulkViewDismissAction with DismissActionInfo
  )

  override protected def viewHolder(config: EpisodeListItemViewHolderConfiguration): View => EpisodeListItemViewHolder =
    new FinishedLibraryEpisodeListItemViewHolder(_, config)
}

private[library] trait StarredFragmentActions extends LibraryEpisodeListFragmentBase {

  override protected def createActions: Map[Int, Action] = super.createActions ++ Map(
    R.id.action_unstar -> new StarEpisodesAction(checkedEpisodes, false) with BulkEpisodeNopUpdate with BulkViewDismissAction with DismissActionInfo,
    R.id.action_mark_finished -> new MarkEpisodesFinishedAction(checkedEpisodes) with BulkEpisodeAdapterUpdate,
    R.id.action_mark_unfinished -> new MarkEpisodesUnfinishedAction(checkedEpisodes) with BulkEpisodeAdapterUpdate
  )

  override protected def viewHolder(config: EpisodeListItemViewHolderConfiguration): View => EpisodeListItemViewHolder =
    new StarredLibraryEpisodeListItemViewHolder(_, config)
}
