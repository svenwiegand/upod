package mobi.upod.app.services.subscription

import android.net.Uri
import com.escalatesoft.subcut.inject.{Injectable, BindingModule}
import java.net.URI
import mobi.upod.android.os.AsyncTask
import mobi.upod.app.App
import mobi.upod.app.services.sync.{SubscriptionSettings, SyncService}
import mobi.upod.app.services.{PodcastWebService, EpisodeService}
import mobi.upod.app.storage.{ImportedSubscriptionsDao, UiPreferences, EpisodeDao, PodcastDao}
import mobi.upod.io
import scala.util.{Failure, Success, Try}
import mobi.upod.android.logging.Logging

class SubscriptionService(implicit val bindingModule: BindingModule) extends Injectable with Logging {
  private lazy val podcastDao = inject[PodcastDao]
  private lazy val episodeDao = inject[EpisodeDao]
  private lazy val syncService = inject[SyncService]
  private lazy val episodeService = inject[EpisodeService]
  private lazy val webService = inject[PodcastWebService]
  private lazy val uiPreferences = inject[UiPreferences]

  def subscribe(podcast: URI): Unit = {
    podcastDao inTransaction {
      podcastDao.subscribe(podcast)
      episodeDao.markUncached(podcast)
      episodeDao.markLatestUnknownEpisodeNew(podcast)
      if (uiPreferences.skipNew) {
        episodeDao.addAllNewEpisodesToLibrary()
      }
    }
    syncService.pushSyncRequired()
    episodeService.fireEpisodeCountChanged()
  }

  def subscribe(podcasts: Traversable[URI]): Unit = {
    podcastDao.inTransaction {
      podcasts.foreach(subscribe)
    }
  }


  def unsubscribe(podcasts: Traversable[URI]): Unit = {
    podcastDao.inTransaction {
      podcasts.foreach { podcast =>
        podcastDao.unsubscribe(podcast)
        episodeDao.deletePodcastNewEpisodes(podcast)
      }
    }
    syncService.pushSyncRequired()
    episodeService.fireEpisodeCountChanged()
  }

  def unsubscribe(podcast: URI): Unit =
    unsubscribe(Traversable(podcast))

  def delete(podcasts: Traversable[URI]): Unit = {
    podcastDao.inTransaction {
      podcasts foreach { podcast =>
        episodeDao.deletePodcastEpisodes(podcast)
        podcastDao.delete(podcast)
      }
    }
    syncService.pushSyncRequired()
    episodeService.fireEpisodeCountChanged()
  }

  def delete(podcast: URI): Unit =
    delete(Traversable(podcast))

  def importOpml(opml: Uri) {
    val podcastUrls = io.forCloseable(inject[App].getContentResolver.openInputStream(opml)) { input =>
      OpmlReader.read(input)
    }
    log.info(s"subscribing to ${podcastUrls.size} podcasts...")

    val importedSubscriptionsDao = inject[ImportedSubscriptionsDao]
    importedSubscriptionsDao.inTransaction(importedSubscriptionsDao.add(podcastUrls))
    syncService.requestFullSync(true)
  }

  def asyncImportOpml(opml: Uri, callback: OpmlImportCallback) {
    AsyncTask.execute[Try[Unit]] {
      Try(importOpml(opml))
    } {
      case Success(_) => callback.onImportSucceeded()
      case Failure(error) => callback.onImportFailed(OpmlException(opml, error))
    }
  }

  def updateSettings(podcast: URI, settings: SubscriptionSettings): Unit = {
    podcastDao.inTransaction(podcastDao.updateSettings(podcast, settings))
    syncService.pushSyncRequired()
    syncService.syncPodcast(podcast)
  }
}
