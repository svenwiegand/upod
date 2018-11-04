package mobi.upod.app.storage

import android.app.Application
import mobi.upod.android.content.preferences._
import mobi.upod.app.gui.MainNavigation

class NavigationPreferences(app: Application, defaultNavItemId: Long, defaultViewModeId: Int) extends DevicePreferences(app) {
  lazy val navItem = new LongPreference("navItem", defaultNavItemId) with Setter[Long]
  lazy val viewMode = new IntPreference("viewMode", defaultViewModeId) with Setter[Int]

  lazy val newEpisodesViewMode = new IntPreference("inboxViewMode", MainNavigation.viewModeIdGroupedEpisodes) with Setter[Int]
  lazy val subscriptionsViewMode = new IntPreference("subscriptionsViewMode", MainNavigation.viewModeIdPodcasts) with Setter[Int]
  lazy val findPodcastsViewMode = new IntPreference("findPodcastsViewMode", MainNavigation.viewModeIdPodcasts) with Setter[Int]
  lazy val playlistViewMode = new IntPreference("playlistViewMode", MainNavigation.viewModeIdEpisodes) with Setter[Int]
  lazy val downloadQueueViewMode = new IntPreference("downloadQueueViewMode", MainNavigation.viewModeIdEpisodes) with Setter[Int]
  lazy val unfinishedEpisodesViewMode = new IntPreference("allEpisodesViewMode", MainNavigation.viewModeIdPodcasts) with Setter[Int]
  lazy val audioEpisodesViewMode = new IntPreference("audioEpisodesViewMode", MainNavigation.viewModeIdPodcasts) with Setter[Int]
  lazy val videoEpisodesViewMode = new IntPreference("videoEpisodesViewMode", MainNavigation.viewModeIdPodcasts) with Setter[Int]
  lazy val downloadedEpisodesViewMode = new IntPreference("downloadedEpisodesViewMode", MainNavigation.viewModeIdPodcasts) with Setter[Int]
  lazy val starredViewMode = new IntPreference("starredViewMode", MainNavigation.viewModeIdPodcasts) with Setter[Int]
  lazy val finishedEpisodesViewMode = new IntPreference("finishedEpisodesViewMode", MainNavigation.viewModeIdGroupedEpisodes) with Setter[Int]

  def preferences = Seq(
    navItem,
    viewMode,
    newEpisodesViewMode,
    subscriptionsViewMode,
    findPodcastsViewMode,
    playlistViewMode,
    downloadQueueViewMode,
    unfinishedEpisodesViewMode,
    starredViewMode,
    finishedEpisodesViewMode
  )

  def recentViewMode(navItemId: Long): IntPreference with Setter[Int] = navItemId match {
    case MainNavigation.`newEpisodes` => newEpisodesViewMode
    case MainNavigation.podcasts => subscriptionsViewMode
    case MainNavigation.findPodcasts => findPodcastsViewMode
    case MainNavigation.playlist => playlistViewMode
    case MainNavigation.downloads => downloadQueueViewMode
    case MainNavigation.unfinishedEpisodes => unfinishedEpisodesViewMode
    case MainNavigation.audioEpisodes => audioEpisodesViewMode
    case MainNavigation.videoEpisodes => videoEpisodesViewMode
    case MainNavigation.downloadedEpisodes => downloadedEpisodesViewMode
    case MainNavigation.starred => starredViewMode
    case MainNavigation.finishedEpisodes => finishedEpisodesViewMode
  }
}
