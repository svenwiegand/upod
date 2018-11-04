package mobi.upod.app.gui.episode.playlist

import android.view.View
import mobi.upod.android.app.ListenerFragment
import mobi.upod.android.app.action._
import mobi.upod.app.R
import mobi.upod.app.data.EpisodeBaseWithPlaybackInfo
import mobi.upod.app.gui.MainNavigation
import mobi.upod.app.gui.episode._
import mobi.upod.app.gui.episode.download.AddEpisodesToDownloadListAction
import mobi.upod.app.gui.episode.library.{SortableLibraryEpisodeListFragment, StarEpisodesAction}
import mobi.upod.app.gui.episode.news.AddEpisodesToLibraryAction
import mobi.upod.app.gui.info.SponsorRequestCardHeaders
import mobi.upod.app.services.playback.{PlaybackListener, PlaybackService}
import mobi.upod.app.storage.EpisodeDao

class PlaylistFragment
  extends SortableLibraryEpisodeListFragment(MainNavigation.playlist, R.string.empty_playlist)
  with SimpleFragmentActions
  with ConfirmedActionProviderFragment
  with PlaybackListener
  with ListenerFragment
  with SponsorRequestCardHeaders {

  private val ActionTagClearList = "clearList"

  private lazy val playService = inject[PlaybackService]
  protected val observables = Traversable(playService)
  private lazy val clearListAction = new RemoveEpisodesFromPlaylistAction(adapter.episodes) with BulkEpisodeAdapterUpdate with ActionWaitDialog with ActionReload

  protected def optionsMenuResourceId = R.menu.playlist_actions
  override protected def hasOptionsMenu: Boolean = enableActions
  override protected val contextualMenuResourceId = R.menu.playlist_episode_contextual
  override protected def createActions: Map[Int, Action] = super.createActions ++ Map(
    R.id.action_add_download -> new AddEpisodesToDownloadListAction(checkedEpisodes) with BulkEpisodeAdapterUpdate,
    R.id.action_star -> new StarEpisodesAction(checkedEpisodes, true) with BulkEpisodeAdapterUpdate,
    R.id.action_unstar -> new StarEpisodesAction(checkedEpisodes, false) with BulkEpisodeAdapterUpdate,
    R.id.action_remove_from_playlist -> new RemoveEpisodesFromPlaylistAction(checkedEpisodes) with BulkEpisodeAdapterUpdate with BulkViewDismissAction with DismissActionInfo,
    R.id.action_mark_read -> new AddEpisodesToLibraryAction(checkedEpisodes) with BulkEpisodeAdapterUpdate,
    R.id.action_mark_finished -> new MarkEpisodesFinishedAction(checkedEpisodes) with BulkEpisodeAdapterUpdate with BulkViewDismissAction with DismissActionInfo,
    R.id.action_mark_unfinished -> new MarkEpisodesUnfinishedAction(checkedEpisodes) with BulkEpisodeAdapterUpdate,
    R.id.action_random_episode -> new AddRandomEpisodeAction,
    R.id.action_clear_list -> new ConfirmedAction(
      R.string.action_clear_list, getString(R.string.action_clear_list_confirmation),
      this, ActionTagClearList)
  )

  override def confirmedAction(tag: String): Action = tag match {
    case ActionTagClearList => clearListAction
  }

  protected def createViewHolder(view: View, config: EpisodeListItemViewHolderConfiguration) =
    new PlaylistEpisodeListItemViewHolder(view, config)

  protected def loadEpisodes(dao: EpisodeDao) = dao.findPlaylistItems

  protected def onEpisodeMoved(from: Int, to: Int, commit: Boolean): Unit = if (commit) {
    playService.asyncUpdatePlaylistPositions(adapter.episodes)
  }

  override protected def invalidateOptionsMenu(): Unit = {
    Option(getActivity).foreach(_.invalidateOptionsMenu())
  }

  //
  // playback listener
  //

  override def onPlaylistChanged() {
    reload()
  }

  private def pinFirstEpisode(pin: Boolean): Unit = {
    if (pin)
      pinFirstEpisodes(1)
    else
      unpinAllEpisodes()
    invalidateOptionsMenu()
  }

  override def onPreparingPlayback(episode: EpisodeBaseWithPlaybackInfo) {
    pinFirstEpisode(true)
  }

  override def onPlaybackStarted(episode: EpisodeBaseWithPlaybackInfo) {
    pinFirstEpisode(true)
  }

  override def onEpisodeCompleted(episode: EpisodeBaseWithPlaybackInfo) {
    pinFirstEpisode(false)
  }

  override def onPlaybackStopped() {
    pinFirstEpisode(false)
  }
}
