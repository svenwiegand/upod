package mobi.upod.app.gui.episode.library

import mobi.upod.app.R
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.gui.episode.GroupedEpisodeListItemAdapter
import mobi.upod.app.gui.info.FinishedEpisodesTip
import mobi.upod.app.gui.{EpisodeListViewModeTip, HideNewPreferenceListenerFragment, MainNavigation}
import mobi.upod.app.storage.EpisodeDao
import mobi.upod.util.Cursor

private[library] sealed abstract
class GroupedLibraryEpisodeListFragment(
    protected val navigationItemId: Long,
    protected val emptyTextId: Int,
    loadItems: EpisodeDao => Cursor[EpisodeListItem])
  extends LibraryEpisodeListFragmentBase {

  protected val viewModeId = MainNavigation.viewModeIdGroupedEpisodes

  protected def createAdapter(data: IndexedSeq[EpisodeListItem]) = {
    def config = ViewHolderConfiguration(false, false, false)
    new GroupedEpisodeListItemAdapter(data, viewHolder(config))
  }

  protected def loadEpisodes(dao: EpisodeDao): Cursor[EpisodeListItem] = loadItems(dao)
}

class UnfinishedGroupedEpisodeListFragment extends GroupedLibraryEpisodeListFragment(MainNavigation.unfinishedEpisodes, R.string.empty_unfinished, _.findGroupedUnfinishedLibraryListItems) with HideNewPreferenceListenerFragment with EpisodeListViewModeTip
class AudioGroupedEpisodeListFragment extends GroupedLibraryEpisodeListFragment(MainNavigation.audioEpisodes, R.string.empty_audio, _.findGroupedUnfinishedAudioLibraryListItems) with HideNewPreferenceListenerFragment with EpisodeListViewModeTip
class VideoGroupedEpisodeListFragment extends GroupedLibraryEpisodeListFragment(MainNavigation.videoEpisodes, R.string.empty_video, _.findGroupedUnfinishedVideoLibraryListItems) with HideNewPreferenceListenerFragment with EpisodeListViewModeTip
class DownloadedGroupedEpisodeListFragment extends GroupedLibraryEpisodeListFragment(MainNavigation.downloadedEpisodes, R.string.empty_downloaded, _.findGroupedUnfinishedDownloadedLibraryListItems) with HideNewPreferenceListenerFragment with EpisodeListViewModeTip
class FinishedGroupedEpisodeListFragment extends GroupedLibraryEpisodeListFragment(MainNavigation.finishedEpisodes, R.string.empty_finished, _.findGroupedRecentlyFinishedLibraryListItems) with FinishedFragmentActions with FinishedEpisodesTip

class StarredGroupedEpisodeListFragment
  extends GroupedLibraryEpisodeListFragment(MainNavigation.starred, R.string.empty_starred, _.findGroupedStarredListItems)
  with StarredFragmentActions
