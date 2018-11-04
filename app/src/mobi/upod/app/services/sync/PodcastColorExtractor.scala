package mobi.upod.app.services.sync

import java.net.{URI, URL}

import com.escalatesoft.subcut.inject.{BindingModule, Injectable}
import mobi.upod.android.logging.Logging
import mobi.upod.app.data.PodcastColors
import mobi.upod.app.storage.{CoverartProvider, ImageSize, PodcastDao}

class PodcastColorExtractor(implicit val bindingModule: BindingModule) extends Injectable with Logging {
  private lazy val podcastDao = inject[PodcastDao]
  private lazy val coverartProvider = inject[CoverartProvider]
  private lazy val syncService = inject[SyncService]

  def extractMissingColors(): Unit = {
    val podcastsWithMissingColors = podcastDao.findWithMissingColor.toSeqAndClose()
    val addedAtLeastOneColor = podcastsWithMissingColors.foldLeft(false) { case (addedColor, (podcast, imageUrl)) =>
        colorizePodcast(podcast, imageUrl).nonEmpty || addedColor
    }
    if (addedAtLeastOneColor) {
      syncService.pushSyncRequired()
    }
  }

  /** Extracts the color from the specified image if any and stores it for the specified podcast.
    *
    * @param podcast the podcast to the image belongs to
    * @param imageUrl the URL of the image to extract the color from
    * @return the extracted podcast colors if any
    */
  def colorizePodcast(podcast: URI, imageUrl: Option[URL], pushSync: Boolean = false): Option[PodcastColors] = {
    val colors = imageUrl.flatMap(extractColors)
    colors foreach { c =>
      podcastDao.inTransaction(podcastDao.updatePodcastColors(podcast, c))
      if (pushSync) {
        syncService.pushSyncRequired()
      }
    }
    colors
  }

  private def extractColors(imageUrl: URL): Option[PodcastColors] =
    coverartProvider.getImageBitmap(imageUrl, ImageSize.list).flatMap(PodcastColors.fromImage)
}
