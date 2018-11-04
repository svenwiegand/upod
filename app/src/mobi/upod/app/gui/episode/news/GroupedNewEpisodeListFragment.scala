package mobi.upod.app.gui.episode.news

import mobi.upod.app.gui.episode.{EpisodeListItemViewHolderConfiguration, GroupedEpisodeListItemAdapter, EpisodeDismissController}
import mobi.upod.app.gui.info.NewEpisodesTip
import mobi.upod.app.storage.EpisodeDao
import mobi.upod.app.gui.{EpisodeListViewModeTip, MainNavigation}
import mobi.upod.app.data.EpisodeListItem

final class GroupedNewEpisodeListFragment extends NewEpisodeListFragmentBase with EpisodeListViewModeTip with NewEpisodesTip {

  protected def loadEpisodes(dao: EpisodeDao) = dao.findGroupedNewListItems

  protected def createAdapter(data: IndexedSeq[EpisodeListItem]) = {
    def config = ViewHolderConfiguration(false, false, false)
    new GroupedEpisodeListItemAdapter(data, new NewEpisodeListItemViewHolder(_, config))
  }

  protected def viewModeId = MainNavigation.viewModeIdGroupedEpisodes
}
