package mobi.upod.app.services

import java.net.{URI, URL}

import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import mobi.upod.android.app.AppException
import mobi.upod.android.logging.Logging
import mobi.upod.app.App
import mobi.upod.app.data._
import mobi.upod.app.services.sync.PodcastFetchService
import mobi.upod.app.storage.{EpisodeDao, PodcastDao}
import mobi.upod.util.Cursor

import scala.util.{Failure, Success, Try}

final class OnlinePodcastService(implicit val bindingModule: BindingModule) extends Injectable with Logging {
  private lazy val app = inject[App]
  private lazy val podcastDao = inject[PodcastDao]
  private lazy val episodeDao = inject[EpisodeDao]
  private lazy val fetchService = inject[PodcastFetchService]

  private def fetchPodcastDetails(url: URL): Try[Podcast] = {

    def cacheEpisode(episode: Episode): Unit = {
      val e = episode.copy(cached = true)
      episodeDao.insertOrIgnore(e)
    }

    def cacheEpisodes(podcastWithEpisodes: PodcastWithEpisodes): Unit = {
      episodeDao.deleteCached()
      val podcastInfo = podcastWithEpisodes.podcast.toEpisodePodcastInfo
      podcastWithEpisodes.episodes foreach (e => cacheEpisode(e.toEpisode(podcastInfo)))
      episodeDao.updatePodcastProperties(podcastWithEpisodes.podcast.uri)
    }

    log.info(s"fetching details for podcast $url")
    try {
      val podcastWithEpisodes = fetchService.fetchPodcast(url)
      podcastDao.inTransaction {
        podcastDao.deleteUnreferenced()
        podcastDao.insertOrIgnore(podcastWithEpisodes.podcast.toPodcast)
        cacheEpisodes(podcastWithEpisodes)
      }
      log.info(s"fetched details for podcast $url")
      podcastDao.find(podcastWithEpisodes.podcast.uri) match {
        case Some(p) => Success(p)
        case None => Success(podcastWithEpisodes.podcast.toPodcast) // should never happen
      }
    } catch {
      case ex: AppException =>
        log.error(s"failed to fetch details for podcast $url", ex)
        app.notifyError(ex)
        Failure(ex)
      case ex: Throwable =>
        log.error(s"failed to fetch details for podcast $url", ex)
        Failure(ex)
    }
  }

  def getPodcastDetails(url: URL): Try[Podcast] = {
    podcastDao.find(url) match {
      case Some(podcast) => Success(podcast)
      case None => fetchPodcastDetails(url)
    }
  }

  def getPodcastDetails(uri: URI, url: URL): Try[Podcast] = {
    podcastDao.find(uri) match {
      case Some(podcast) => Success(podcast)
      case None => fetchPodcastDetails(url)
    }
  }

  def getCachedOnlinePodcastEpisodes(podcast: PodcastListItem, sortAscending: Boolean): Cursor[EpisodeListItem] =
    episodeDao.findOnlinePodcastListItems(podcast.id, sortAscending)
}
