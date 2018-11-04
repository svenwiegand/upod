package mobi.upod.app.gui.episode

import android.view.View
import android.widget.ListView
import mobi.upod.app.R
import mobi.upod.app.gui.{MainNavigation, PodcastEpisodesActivity}

trait OfflinePodcastEpisodeListFragment extends PodcastEpisodeListFragment {

  private lazy val footerView = View.inflate(getActivity, R.layout.episode_list_footer, null)
  private lazy val showOtherPodcastEpisodesButton = footerView.childAs[View](R.id.showOtherPodcastEpisodes)

  override protected val hasFooters: Boolean = true

  override protected def onAddFooters(listView: ListView): Unit = {
    getListView.addFooterView(footerView, null, false)
    showOtherPodcastEpisodesButton.onClick(showOtherPodcastEpisodes())
    super.onAddFooters(listView)
  }

  private def showOtherPodcastEpisodes(): Unit = {
    PodcastEpisodesActivity.start(getActivity, MainNavigation.podcasts, podcast)

  }
}
