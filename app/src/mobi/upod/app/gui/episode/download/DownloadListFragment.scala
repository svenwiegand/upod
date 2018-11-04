package mobi.upod.app.gui.episode.download

import android.view.View
import mobi.upod.android.app.ListenerFragment
import mobi.upod.android.app.action._
import mobi.upod.app.R
import mobi.upod.app.data.EpisodeBaseWithDownloadInfo
import mobi.upod.app.gui.MainNavigation
import mobi.upod.app.gui.episode.library.{SortableLibraryEpisodeListFragment, StarEpisodesAction}
import mobi.upod.app.gui.episode.news.AddEpisodesToLibraryAction
import mobi.upod.app.gui.episode.playlist.AddEpisodesToPlaylistAction
import mobi.upod.app.gui.episode.{BulkViewDismissAction, EpisodeListItemViewHolderConfiguration, MarkEpisodesFinishedAction, MarkEpisodesUnfinishedAction}
import mobi.upod.app.gui.info.DownloadQueueTips
import mobi.upod.app.services.download.{DownloadListAction, DownloadListener, DownloadService, StopDownloadAction}
import mobi.upod.app.storage.EpisodeDao

class DownloadListFragment
  extends SortableLibraryEpisodeListFragment(MainNavigation.downloads, R.string.empty_download_list)
  with SimpleFragmentActions
  with ConfirmedActionProviderFragment
  with DownloadQueueTips
  with DownloadListener
  with ListenerFragment {

  private val ActionTagClearList = "clearList"

  private lazy val downloadService = inject[DownloadService]
  protected val observables = Traversable(downloadService)
  private lazy val clearListAction = new RemoveEpisodesFromDownloadListAction(adapter.episodes) with BulkEpisodeAdapterUpdate with ActionWaitDialog with ActionReload

  override protected val optionsMenuResourceId = R.menu.downloads_actions
  override protected def hasOptionsMenu: Boolean = enableActions
  override protected val contextualMenuResourceId = R.menu.downloads_episode_contextual
  override protected def createActions: Map[Int, Action] = super.createActions ++ Map(
    R.id.action_download_list -> new DownloadListAction,
    R.id.action_stop_download -> new StopDownloadAction,
    R.id.action_add_to_playlist -> new AddEpisodesToPlaylistAction(checkedEpisodes) with BulkEpisodeAdapterUpdate,
    R.id.action_star -> new StarEpisodesAction(checkedEpisodes, true) with BulkEpisodeAdapterUpdate,
    R.id.action_unstar -> new StarEpisodesAction(checkedEpisodes, false) with BulkEpisodeAdapterUpdate,
    R.id.action_remove_from_download_list -> new RemoveEpisodesFromDownloadListAction(checkedEpisodes) with BulkEpisodeAdapterUpdate with BulkViewDismissAction with DismissActionInfo,
    R.id.action_mark_read -> new AddEpisodesToLibraryAction(checkedEpisodes) with BulkEpisodeAdapterUpdate,
    R.id.action_mark_finished -> new MarkEpisodesFinishedAction(checkedEpisodes) with BulkEpisodeAdapterUpdate with BulkViewDismissAction with DismissActionInfo,
    R.id.action_mark_unfinished -> new MarkEpisodesUnfinishedAction(checkedEpisodes) with BulkEpisodeAdapterUpdate,
    R.id.action_clear_list -> new ConfirmedAction(
      R.string.action_clear_list, getString(R.string.action_clear_list_confirmation),
      this, ActionTagClearList)
  )

  override def confirmedAction(tag: String): Action = tag match {
    case ActionTagClearList => clearListAction
  }

  protected def createViewHolder(view: View, config: EpisodeListItemViewHolderConfiguration) =
    new DownloadListEpisodeListItemViewHolder(view, config)

  protected def loadEpisodes(dao: EpisodeDao) = dao.findDownloadListItems

  protected def onEpisodeMoved(from: Int, to: Int, commit: Boolean): Unit = if (commit) {
    downloadService.asyncUpdateDownloadList(adapter.episodes)
  }

  override protected def invalidateOptionsMenu(): Unit = {
    Option(getActivity).foreach(_.invalidateOptionsMenu())
  }

  //
  // download listener
  //

  override def onDownloadListChanged() {
    reload()
    pinFirstEpisodes(downloadService.immediateDownloadQueue.size)
  }

  override def onDownloadStarted(episode: EpisodeBaseWithDownloadInfo) {
    pinFirstEpisodes(downloadService.immediateDownloadQueue.size)
    invalidateOptionsMenu()
  }

  override def onDownloadStopped(episode: EpisodeBaseWithDownloadInfo) {
    pinFirstEpisodes(downloadService.immediateDownloadQueue.size)
    invalidateOptionsMenu()
  }
}
