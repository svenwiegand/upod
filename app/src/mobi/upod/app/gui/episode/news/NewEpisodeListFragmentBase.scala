package mobi.upod.app.gui.episode.news

import mobi.upod.android.app.action.{ActionWaitDialog, Action, SimpleFragmentActions}
import mobi.upod.app.R
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.gui.episode._
import mobi.upod.app.gui.episode.download.AddEpisodesToDownloadListAction
import mobi.upod.app.gui.episode.playlist.AddEpisodesToPlaylistAction
import mobi.upod.app.gui.{ReloadOnEpisodeListChangedFragment, MainNavigation}
import mobi.upod.app.services.sync.NewEpisodesNotification
import mobi.upod.app.storage.EpisodeDao

private[news] abstract class NewEpisodeListFragmentBase
  extends EpisodeListFragment
  with ReloadOnEpisodeListChangedFragment[IndexedSeq[EpisodeListItem]]
  with SimpleFragmentActions {

  protected lazy val episodeDao = inject[EpisodeDao]

  protected val optionsMenuResourceId = R.menu.new_actions

  protected val emptyTextId: Int = R.string.empty_new

  override protected def hasOptionsMenu: Boolean = enableActions

  protected val contextualMenuResourceId = R.menu.new_episode_contextual
  protected override def createActions: Map[Int, Action] = super.createActions ++ Map(
    R.id.action_mark_all_read -> new AddEpisodesToLibraryAction(adapter.episodes) with BulkEpisodeAdapterUpdate with ActionWaitDialog with BulkViewDismissAction with DismissActionInfo,
    R.id.action_mark_all_finished -> new MarkEpisodesFinishedAction(adapter.episodes) with BulkEpisodeAdapterUpdate with ActionWaitDialog with BulkViewDismissAction with DismissActionInfo,
    R.id.action_mark_read -> new AddEpisodesToLibraryAction(checkedEpisodes) with BulkEpisodeAdapterUpdate with BulkViewDismissAction with DismissActionInfo,
    R.id.action_add_download -> new AddEpisodesToDownloadListAction(checkedEpisodes) with BulkEpisodeAdapterUpdate with BulkViewDismissAction with DismissActionInfo,
    R.id.action_add_to_playlist -> new AddEpisodesToPlaylistAction(checkedEpisodes) with BulkEpisodeAdapterUpdate with BulkViewDismissAction with DismissActionInfo,
    R.id.action_mark_finished -> new MarkEpisodesFinishedAction(checkedEpisodes) with BulkEpisodeAdapterUpdate with BulkViewDismissAction with DismissActionInfo,
    R.id.action_mark_unfinished -> new MarkEpisodesUnfinishedAction(checkedEpisodes) with BulkEpisodeAdapterUpdate with BulkViewDismissAction with DismissActionInfo
  )

  protected def navigationItemId = MainNavigation.newEpisodes

  override def onResume() = {
    super.onResume()
    NewEpisodesNotification.cancel(app)
  }
}
