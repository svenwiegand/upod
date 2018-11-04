package mobi.upod.app.gui.podcast

import java.net.URL

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.ActionBarActivity
import mobi.upod.android.app.{ActivityStateHolder, WaitDialogFragment}
import mobi.upod.android.logging.Logging
import mobi.upod.android.os.AsyncTask
import mobi.upod.app.data.{Podcast, PodcastListItem}
import mobi.upod.app.gui.PodcastEpisodesActivity
import mobi.upod.app.services.sync.{PodcastColorExtractor, ImageFetcher}
import mobi.upod.app.storage.{ImageSize, CoverartProvider}
import mobi.upod.app.{AppInjection, R}

import scala.util.{Failure, Success, Try}

private[podcast] abstract class OpenPodcastActivity
  extends ActionBarActivity
  with ActivityStateHolder
  with AppInjection
  with Logging {

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    fetchPodcastDetails()
  }

  protected def validateIntentData(): Boolean = true

  protected def fetchPodcast: Try[Podcast]

  protected def fetchPodcastWithColors: Try[Podcast] = {

    def ensureCoverartAvailable(imageUrl: Option[URL]): Unit = imageUrl foreach { url =>
      if (inject[CoverartProvider].getExistingImageFile(url, ImageSize.full).isEmpty) {
        new ImageFetcher().fetchImage(url)
      }
    }

    def podcastWithExtractedColors(podcast: Podcast): Podcast = {
      new PodcastColorExtractor().colorizePodcast(podcast.uri, podcast.imageUrl, true) match {
        case Some(colors) => podcast.copy(colors = Some(colors))
        case _ => podcast
      }
    }

    def podcastWithColors(podcast: Podcast): Podcast = podcast.colors match {
      case Some(colors) =>
        podcast
      case None =>
        ensureCoverartAvailable(podcast.imageUrl)
        podcastWithExtractedColors(podcast)
    }
    
    val podcast = fetchPodcast
    podcast.map(podcastWithColors)
  }

  protected def onPodcastError(error: Throwable): Unit

  private def fetchPodcastDetails() {
    if (validateIntentData()) {
      WaitDialogFragment.show(this, R.string.open_podcast_url_progress)
      AsyncTask.execute(fetchPodcastWithColors) { podcast =>
        WaitDialogFragment.dismiss(this)
        podcast match {
          case Success(p) => showPodcast(p)
          case Failure(error) => onPodcastError(error)
        }
      }
    }
  }

  protected def navigationItemId: Long

  private def showPodcast(podcast: Podcast) {
    PodcastEpisodesActivity.start(this, navigationItemId, PodcastListItem(podcast),
      Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP, false)
    finish()
  }
}
