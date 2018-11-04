package mobi.upod.app.services.sync

import java.net.URL

import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndEntry
import mobi.upod.android.logging.Logging
import mobi.upod.app.data.PodcastWithEpisodes
import mobi.upod.net.UserAgent
import mobi.upod.syndication.{FeedWithCacheInfo, CacheInfo, ProgressiveFeedFetcher}
import org.joda.time.DateTime

import scala.collection.JavaConverters._

class PodcastFetchService(implicit val bindingModule: BindingModule) extends Injectable with Logging {

  def fetchPodcast(url: URL): PodcastWithEpisodes =
    fetchPodcastIfNewOrUpdated(url, None, None).get

  def fetchPodcastIfNewOrUpdated(url: URL, eTag: Option[String], lastModified: Option[DateTime]): Option[PodcastWithEpisodes] = {

    def logFetchResult(url: URL, feed: Option[FeedWithCacheInfo]): Unit = {
      feed match {
        case Some(f) if !f.newEpisodesOnly =>
          log.info(s"podcast '$url' was fully retrieved with eTag ${f.cacheInfo.eTag} and modified date ${f.cacheInfo.lastModified}")
        case Some(f) =>
          log.info(s"podcast '$url' was partly retrieved with ${f.feed.getEntries.size()} episodes")
        case None =>
          log.info(s"podcast '$url' is unchanged")
      }
    }

    def buildPodcast(url: URL, feed: FeedWithCacheInfo): PodcastWithEpisodes = {
      val podcast = PodcastParser.createPodcast(feed.feed, url, Some(feed.cacheInfo))
      val episodes = feed.feed.getEntries.asInstanceOf[java.util.List[SyndEntry]]
      PodcastWithEpisodes(podcast, EpisodeParser.list(podcast, episodes.asScala.toIndexedSeq))
    }

    val fetchResult = new ProgressiveFeedFetcher(UserAgent.Name).retrieveFeedIfChanged(url, CacheInfo(eTag, lastModified))
    logFetchResult(url, fetchResult)
    fetchResult match {
      case Some(feed) => Some(buildPodcast(url, feed))
      case None => None
    }
  }
}
