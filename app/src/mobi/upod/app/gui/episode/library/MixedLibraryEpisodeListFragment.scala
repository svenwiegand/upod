package mobi.upod.app.gui.episode.library

import mobi.upod.android.app.action.{SimpleFragmentActions, Action}
import mobi.upod.app.R
import mobi.upod.app.data.{EpisodeListItem, PodcastListItem}
import mobi.upod.app.gui.episode.download.{DeleteRecentlyFinishedPodcastEpisodesAction, DeleteRecentlyFinishedEpisodesAction}
import mobi.upod.app.gui.info.FinishedEpisodesTip
import mobi.upod.app.gui.{EpisodeListViewModeTip, HideNewPreferenceListenerFragment, MainNavigation}
import mobi.upod.app.gui.episode._
import mobi.upod.app.storage.EpisodeDao
import mobi.upod.util.Cursor

private[library] sealed abstract class MixedLibraryEpisodeListFragment(
    protected val navigationItemId: Long,
    protected val emptyTextId: Int,
    showItemPodcastTitle: Boolean,
    singlePodcastList: Boolean)
  extends LibraryEpisodeListFragmentBase {

  protected def createAdapter(data: IndexedSeq[EpisodeListItem]) = {
    def config = ViewHolderConfiguration(showItemPodcastTitle, singlePodcastList, false)
    new EpisodeListItemAdapter(data, viewHolder(config))
  }
}

private[library] sealed abstract
class LibraryEpisodeListFragment(
    navItemId: Long,
    emptyTextId: Int,
    loadItems: EpisodeDao => Cursor[EpisodeListItem])
  extends MixedLibraryEpisodeListFragment(navItemId, emptyTextId, true, false)
  with EpisodeListViewModeTip {

  protected def loadEpisodes(dao: EpisodeDao) = loadItems(dao)

  protected val viewModeId = MainNavigation.viewModeIdEpisodes
}

class UnfinishedEpisodeListFragment extends LibraryEpisodeListFragment(MainNavigation.unfinishedEpisodes, R.string.empty_unfinished, _.findUnfinishedLibraryListItems) with HideNewPreferenceListenerFragment with EpisodeListViewModeTip
class AudioEpisodeListFragment extends LibraryEpisodeListFragment(MainNavigation.audioEpisodes, R.string.empty_audio, _.findUnfinishedAudioLibraryListItems) with HideNewPreferenceListenerFragment with EpisodeListViewModeTip
class VideoEpisodeListFragment extends LibraryEpisodeListFragment(MainNavigation.videoEpisodes, R.string.empty_video, _.findUnfinishedVideoLibraryListItems) with HideNewPreferenceListenerFragment with EpisodeListViewModeTip
class DownloadedEpisodeListFragment extends LibraryEpisodeListFragment(MainNavigation.downloadedEpisodes, R.string.empty_downloaded, _.findUnfinishedDownloadedLibraryListItems) with HideNewPreferenceListenerFragment with EpisodeListViewModeTip
class FinishedEpisodeListFragment extends LibraryEpisodeListFragment(MainNavigation.finishedEpisodes, R.string.empty_finished, _.findRecentlyFinishedLibraryListItems) with FinishedFragmentActions with FinishedEpisodesTip

class StarredEpisodeListFragment
  extends LibraryEpisodeListFragment(MainNavigation.starred, R.string.empty_starred, _.findStarredListItems)
  with StarredFragmentActions

private[library] sealed abstract
class LibraryPodcastEpisodeListFragment(
    navItemId: Long,
    emptyTextId: Int,
    loadItems: (EpisodeDao, Long, Boolean) => Cursor[EpisodeListItem])
  extends MixedLibraryEpisodeListFragment(navItemId, emptyTextId, false, true) with OfflinePodcastEpisodeListFragment with SimpleFragmentActions {

  protected val optionsMenuResourceId: Int = R.menu.library_podcast_episode_actions

  override protected def hasOptionsMenu: Boolean = enableActions

  override protected def createActions: Map[Int, Action] = podcastActions ++ super.createActions

  protected def loadEpisodes(dao: EpisodeDao, podcast: PodcastListItem, sortAscending: Boolean): Cursor[EpisodeListItem] =
    loadItems(dao, podcast.id, sortAscending)
}

class UnfinishedPodcastEpisodeListFragment extends LibraryPodcastEpisodeListFragment(MainNavigation.unfinishedEpisodes, R.string.empty_unfinished, _.findUnfinishedLibraryListItems(_, _)) with HideNewPreferenceListenerFragment
class AudioPodcastEpisodeListFragment extends LibraryPodcastEpisodeListFragment(MainNavigation.audioEpisodes, R.string.empty_audio, _.findUnfinishedAudioLibraryListItems(_, _)) with HideNewPreferenceListenerFragment
class VideoPodcastEpisodeListFragment extends LibraryPodcastEpisodeListFragment(MainNavigation.videoEpisodes, R.string.empty_video, _.findUnfinishedVideoLibraryListItems(_, _)) with HideNewPreferenceListenerFragment
class DownloadedPodcastEpisodeListFragment extends LibraryPodcastEpisodeListFragment(MainNavigation.downloadedEpisodes, R.string.empty_downloaded, _.findUnfinishedDownloadedLibraryListItems(_, _)) with HideNewPreferenceListenerFragment
class FinishedPodcastEpisodeListFragment extends LibraryPodcastEpisodeListFragment(MainNavigation.finishedEpisodes, R.string.empty_finished, _.findRecentlyFinishedLibraryListItems(_, _)) with FinishedFragmentActions {

  override protected def createActions: Map[Int, Action] = super.createActions ++ Map(
    R.id.action_delete_recently_finished -> new DeleteRecentlyFinishedPodcastEpisodesAction(podcast, optionalAdapter.exists(_.episodes.nonEmpty)) with ActionReload
  )
}

class StarredPodcastEpisodeListFragment
  extends LibraryPodcastEpisodeListFragment(MainNavigation.starred, R.string.empty_starred, _.findStarredListItems(_, _))
  with StarredFragmentActions

