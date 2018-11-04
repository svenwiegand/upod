package mobi.upod.app.gui.episode.news

import mobi.upod.android.app.action.Action
import mobi.upod.app.R
import mobi.upod.app.data.{EpisodeListItem, PodcastListItem}
import mobi.upod.app.gui.episode.{EpisodeListItemAdapter, OfflinePodcastEpisodeListFragment}
import mobi.upod.app.gui.info.NewEpisodesTip
import mobi.upod.app.gui.{EpisodeListViewModeTip, MainNavigation}
import mobi.upod.app.storage.EpisodeDao
import mobi.upod.util.Cursor

private[news] sealed abstract class MixedNewEpisodeListFragment(showItemPodcastTitle: Boolean, singlePodcastList: Boolean)
  extends NewEpisodeListFragmentBase {

  protected def createAdapter(data: IndexedSeq[EpisodeListItem]) = {
    def config = ViewHolderConfiguration(showItemPodcastTitle, singlePodcastList, false)
    new EpisodeListItemAdapter(data, new NewEpisodeListItemViewHolder(_, config))
  }
}

class NewEpisodeListFragment extends MixedNewEpisodeListFragment(true, false) with EpisodeListViewModeTip with NewEpisodesTip {
  protected val viewModeId: Int = MainNavigation.viewModeIdEpisodes

  protected def loadEpisodes(dao: EpisodeDao): Cursor[EpisodeListItem] = dao.findNewListItems
}

class NewPodcastEpisodeListFragment extends MixedNewEpisodeListFragment(false, true) with OfflinePodcastEpisodeListFragment {
  override protected val optionsMenuResourceId = R.menu.new_podcast_episode_actions
  override protected def createActions: Map[Int, Action] = podcastActions ++ super.createActions

  protected def loadEpisodes(dao: EpisodeDao, podcast: PodcastListItem, sortAscending: Boolean): Cursor[EpisodeListItem] =
    dao.findNewListItems(podcast.id, sortAscending)
}
