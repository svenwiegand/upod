package mobi.upod.app.gui.podcast

import mobi.upod.app.data.PodcastListItem
import mobi.upod.app.gui.info.{NewEpisodesTip, FinishedEpisodesTip}
import mobi.upod.app.services.subscription.ExportSubscriptionsAction
import mobi.upod.app.storage.PodcastDao
import mobi.upod.util.Cursor
import mobi.upod.app.gui.{SyncOnPull, EpisodeListViewModeTip, HideNewPreferenceListenerFragment, MainNavigation}
import mobi.upod.app.R
import mobi.upod.android.app.action._
import mobi.upod.app.services.sync.NewEpisodesNotification
import android.os.Bundle
import mobi.upod.android.content.AsyncCursorLoader

sealed private[podcast] abstract class DatabasePodcastGridFragment(
    navId: Long,
    emptyTextId: Int,
    loadPodcasts: PodcastDao => Cursor[PodcastListItem])
  extends PodcastGridFragment(navId, emptyTextId)
  with ConfirmedActionProviderFragment
  with SyncOnPull {

  private val ActionTagDeletePodcast = "deletePodcast"
  private lazy val podcastDao = inject[PodcastDao]
  private lazy val deletePodcastAction = new DeletePodcastsAction(checkedPodcasts) with ActionWaitDialog with ImmediateReload

  protected def createActions = Map[Int, Action](
    R.id.action_podcast_subscribe -> new SubscribeToPodcastsAction(checkedPodcasts) with ActionWaitDialog with BulkPodcastAdapterUpdate,
    R.id.action_podcast_unsubscribe -> new UnsubscribeFromPodcastsAction(checkedPodcasts) with ActionWaitDialog with ImmediateReload,
    R.id.action_podcast_delete -> new ConfirmedAction(
      R.string.action_podcast_delete,
      getResources.getQuantityString(R.plurals.confirm_delete_podcast, checkedPodcasts.size),
      this, ActionTagDeletePodcast)
  )

  override def confirmedAction(tag: String): Action = tag match {
    case ActionTagDeletePodcast => deletePodcastAction
  }

  def onCreateLoader(id: Int, args: Bundle) =
    AsyncCursorLoader(getActivity, loadPodcasts(podcastDao))
}

class NewPodcastsGridFragment
  extends DatabasePodcastGridFragment(MainNavigation.newEpisodes, R.string.empty_new, _.findNewListItems)
  with EpisodeListViewModeTip
  with NewEpisodesTip {

  override def onResume() = {
    super.onResume()
    NewEpisodesNotification.cancel(app)
  }
}

class UnfinishedPodcastsGridFragment extends DatabasePodcastGridFragment(MainNavigation.unfinishedEpisodes, R.string.empty_unfinished, _.findUnfinishedLibraryListItems) with HideNewPreferenceListenerFragment with EpisodeListViewModeTip
class AudioPodcastsGridFragment extends DatabasePodcastGridFragment(MainNavigation.audioEpisodes, R.string.empty_audio, _.findUnfinishedAudioLibraryListItems) with HideNewPreferenceListenerFragment with EpisodeListViewModeTip
class VideoPodcastsGridFragment extends DatabasePodcastGridFragment(MainNavigation.videoEpisodes, R.string.empty_video, _.findUnfinishedVideoLibraryListItems) with HideNewPreferenceListenerFragment with EpisodeListViewModeTip
class DownloadedPodcastsGridFragment extends DatabasePodcastGridFragment(MainNavigation.downloadedEpisodes, R.string.empty_downloaded, _.findUnfinishedDownloadedLibraryListItems) with HideNewPreferenceListenerFragment with EpisodeListViewModeTip
class StarredPodcastsGridFragment extends DatabasePodcastGridFragment(MainNavigation.starred, R.string.empty_starred, _.findStarredListItems)
class FinishedPodcastsGridFragment extends DatabasePodcastGridFragment(MainNavigation.finishedEpisodes, R.string.empty_finished, _.findRecentlyFinishedLibraryListItems) with FinishedEpisodesTip

class AllPodcastsGridFragment
  extends DatabasePodcastGridFragment(MainNavigation.podcasts, R.string.empty_unfinished, { podcastDao =>
    podcastDao.inTransaction(podcastDao.deleteUnreferenced())
    podcastDao.findPodcastListItems
  })
  with SimpleFragmentActions {

  protected def optionsMenuResourceId = R.menu.all_podcasts_actions

  override protected def createActions = super.createActions ++ Map(
    R.id.action_export_subscriptions -> new ExportSubscriptionsAction
  )

  override def onActivityCreated(savedInstanceState: Bundle): Unit = {
    super.onActivityCreated(savedInstanceState)
    AddPodcastFabHelper.add(this)
  }
}