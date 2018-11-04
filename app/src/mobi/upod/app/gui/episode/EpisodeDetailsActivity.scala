package mobi.upod.app.gui.episode

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.NavUtils
import android.widget.FrameLayout
import mobi.upod.android.app.FragmentTransactions._
import mobi.upod.android.app.{NavigationItemSelection, ViewModeSelection, UpNavigation}
import mobi.upod.android.content.IntentHelpers._
import mobi.upod.android.view.ChildViews
import mobi.upod.app.data.EpisodeListItem
import mobi.upod.app.gui.{ActivityWithPlaybackPanelAndStandardActions, MainNavigation, ParentActivityIntent}
import mobi.upod.app.{R, AppInjection}

class EpisodeDetailsActivity
  extends ActivityWithPlaybackPanelAndStandardActions
  with EpisodeListHolder
  with EpisodeDetailsHolder
  with UpNavigation
  with AppInjection
  with ChildViews {
  private lazy val detailsFragment =
    getFragmentManager.findFragmentById(R.id.episode_details).asInstanceOf[EpisodeDetailsFragment]

  private def episodeListItem = getIntent.getExtra(EpisodeSelection).get

  private def parentActivityIntent = getIntent.getExtra(ParentActivityIntent).get

  def navigationItemId = getIntent.getExtra(NavigationItemSelection)

  private def viewModeId = getIntent.getExtra(ViewModeSelection)

  private def episodeListFragment: Option[EpisodeListFragment] =
    Option(getFragmentManager.findFragmentById(R.id.episodes).asInstanceOf[EpisodeListFragment])

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.episode_details)
    showNavigationFragmentIfApplicable()
    updateTitle()
    updateDetailsFragment()
    createPlaybackPanel()
  }

  private def updateTitle() {
    setTitle(episodeListItem.title)
    getSupportActionBar.setSubtitle(episodeListItem.podcastInfo.title)
  }

  private def showNavigationFragmentIfApplicable() {
    optionalChildAs[FrameLayout](R.id.episodes) foreach  { container =>
      getFragmentManager.inTransaction(_.addTransient(R.id.episodes, createNavigationFragment))
    }
  }

  private def createNavigationFragment: EpisodeListFragment =
    MainNavigation.createFragment(navigationItemId, viewModeId).asInstanceOf[EpisodeListFragment]

  private def updateDetailsFragment() {
    detailsFragment.setEpisode(episodeListItem)
  }

  protected def navigateUp() {
    NavUtils.navigateUpTo(this, parentActivityIntent)
  }

  def closeListView(): Unit = {
    onBackPressed()
  }

  val enableEpisodeActions = false

  val checkClickedEpisode = true

  def checkedEpisode = Some(episodeListItem)

  def openEpisode(episodeListItem: EpisodeListItem, navigationItemId: Long, viewModeId: Int, prepareIntent: Intent => Unit) {
    openEpisode(episodeListItem)
  }

  def openEpisode(episodeListItem: EpisodeListItem): Unit = {
    detailsFragment.setEpisode(episodeListItem)
    getIntent.putExtra(EpisodeSelection, episodeListItem)
    updateTitle()
  }

  def removeEpisodeFromList(episode: EpisodeListItem): Unit = {
    episodeListFragment match {
      case Some(episodeList) =>
        val selectedEpisode = episodeList.removeEpisode(episode)
        selectedEpisode match {
          case Some(e) => openEpisode(e)
          case None => finish()
        }
      case None =>
        finish()
    }
  }
}

object EpisodeDetailsActivity {

  private def buildActivityIntent(activity: Activity): Intent = {
    val intent = new Intent(activity, activity.getClass)
    intent.putExtras(activity.getIntent)
  }

  def start(context: Activity, episode: EpisodeListItem, navigationItemId: Long, viewModeId: Int, prepareIntent: Intent => Unit) {
    val intent = new Intent(context, classOf[EpisodeDetailsActivity])
    intent.putExtras(context.getIntent)
    intent.putExtra(ParentActivityIntent, buildActivityIntent(context))
    intent.putExtra(EpisodeSelection, episode)
    intent.putExtra(NavigationItemSelection, navigationItemId)
    intent.putExtra(ViewModeSelection, viewModeId)
    intent.putExtra(SuppressPodcastHeader, true)
    prepareIntent(intent)
    context.startActivity(intent)
  }
}
