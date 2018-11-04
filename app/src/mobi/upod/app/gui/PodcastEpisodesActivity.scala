package mobi.upod.app.gui

import android.content.{Context, Intent}
import android.os.Bundle
import mobi.upod.android.app.FragmentTransactions._
import mobi.upod.android.app.{LabeledNavigationDrawerEntry, ActionModeTracking, NavigationItemSelection, StandardUpNavigation}
import mobi.upod.android.content.IntentHelpers._
import mobi.upod.android.logging.Logging
import mobi.upod.android.os.BundleBooleanValue
import mobi.upod.app.data.PodcastListItem
import mobi.upod.app.gui.episode.{PodcastEpisodeListFragment, FullFeaturedEpisodeListHolderActivity}
import mobi.upod.app.gui.podcast.{PodcastListFragment, PodcastSelectionListener}
import mobi.upod.app.{IntentExtraKey, AppInjection, R}

class PodcastEpisodesActivity
  extends ActivityWithPlaybackPanelAndStandardActions
  with ActionModeTracking
  with PodcastSelectionListener
  with FullFeaturedEpisodeListHolderActivity
  with StandardUpNavigation
  with AppInjection {

  private def navigationItemId = getIntent.getExtra(NavigationItemSelection)

  private def navigationItem = MainNavigation.itemsById(navigationItemId)

  private def selectedPodcast = getIntent.getExtra(PodcastSelection).get

  private def showPodcastList = getIntent.getExtra(PodcastEpisodesActivity.ShowPodcastList)

  private var episodeListFragment: Option[PodcastEpisodeListFragment] = None

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    val layoutId = if (showPodcastList) R.layout.podcast_episodes else R.layout.podcast_episodes_without_podcasts
    setContentView(layoutId)
    createPlaybackPanel()

    if (getIntent.getExtra(PodcastSelection).isEmpty) {
      // should never happen, but it does so in practice
      log.crashLogError(s"podcast selection is empty for navigation $navigationItemId though it shouldn't")
      finish()
    } else {
      updateTitle()
      showFragment()
    }
  }

  private def updateTitle() {
    setTitle(selectedPodcast.title)
    navigationItem match {
      case item: LabeledNavigationDrawerEntry => getSupportActionBar.setSubtitle(item.titleId)
      case _ => // ignore
    }
  }

  private def showFragment() {
    Option(getFragmentManager.findFragmentById(R.id.main_view).asInstanceOf[PodcastEpisodeListFragment]) match {
      case Some(fragment) =>
        episodeListFragment = Some(fragment)
      case None =>
        episodeListFragment = Some(createFragment)
        getFragmentManager.inTransaction(_.add(R.id.main_view, episodeListFragment.get))
    }
  }

  private def createFragment: PodcastEpisodeListFragment =
    MainNavigation.createFragment(navigationItemId, MainNavigation.viewModeIdPodcastEpisodes).asInstanceOf[PodcastEpisodeListFragment]

  def onPodcastSelected(podcast: PodcastListItem) {
    val intent = getIntent
    intent.putExtra(PodcastSelection, podcast)
    setIntent(intent)
    updateTitle()
    ensureActionModeFinished()
    episodeListFragment.foreach(_.onPodcastChanged())
  }

  private def podcastListFragment: Option[PodcastListFragment] =
    Option(getFragmentManager.findFragmentById(R.id.podcastList).asInstanceOf[PodcastListFragment])

  def closeListView(): Unit = {
    podcastListFragment match {
      case Some(podcastList) =>
        if (!podcastList.removeSelected()) {
          onBackPressed()
        }
      case None =>
        onBackPressed()
    }
  }
}

object PodcastEpisodesActivity extends Logging {

  private object ShowPodcastList extends BundleBooleanValue(IntentExtraKey("showPodcastList"))

  def start(context: Context, navigationItemId: Long, podcast: PodcastListItem, additionalIntentFlags: Int = 0, showPodcastList: Boolean = true): Unit = {
    if (podcast == null) {
      val ex = new NullPointerException("podcast is null though it shouldn't")
      log.crashLogError("podcast is null though it shouldn't", ex)
    } else {
      val intent = new Intent(context, classOf[PodcastEpisodesActivity])
      intent.addFlags(additionalIntentFlags)
      intent.putExtra(NavigationItemSelection, navigationItemId)
      intent.putExtra(PodcastSelection, podcast)
      intent.putExtra(ShowPodcastList, showPodcastList)
      context.startActivity(intent)
    }
  }
}