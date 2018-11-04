package mobi.upod.app.data

import java.io.File
import java.net.{URL, URI}
import mobi.upod.app.storage.StorageProvider
import mobi.upod.io._
import mobi.upod.net._
import mobi.upod.util.Hash
import mobi.upod.util.Permille._
import org.joda.time.DateTime
import java.text.SimpleDateFormat
import mobi.upod.android.logging.Logger

trait EpisodeBaseWithDownloadInfo extends EpisodeBase {
  val downloadInfo: EpisodeDownloadInfo

  def oldMediaFile(storageProvider: StorageProvider): File = {
    val baseDir = storageProvider.podcastDirectory
    val podcastDir = new File(baseDir, podcastInfo.title.fileNameEncoded)
    podcastDir.mkdirs()
    val extension = media.url.fileExtension.map(extension => s".$extension").getOrElse("").fileNameEncoded
    new File(podcastDir, title.fileNameEncoded + "_" + EpisodeBaseWithDownloadInfo.mediaFileDateFormat(published) + extension)
  }

  def mediaFile(storageProvider: StorageProvider): File = {
    val baseDir = storageProvider.podcastDirectory
    val file = new File(baseDir, downloadInfo.file.getOrElse(
      EpisodeBaseWithDownloadInfo.mediaFile(media.url, podcast, podcastInfo.title, uri, title, published)))
    val podcastDir = file.getParentFile
    podcastDir.mkdirs()
    file
  }

  def deleteMediaFile(storageProvider: StorageProvider): Unit = {
    val file = mediaFile(storageProvider)
    log.crashLogInfo(s"deleting media file $file")
    file.delete()
    file.deleteParentIfEmpty()
  }

  def estimateDownloadedDuration: Option[Long] = {
    if (media.duration > 0 &&  media.length > 0) {
      Some(media.duration * downloadInfo.fetchedBytes.permille(media.length) / PermilleMax)
    } else {
      None
    }
  }

  private lazy val log = new Logger(getClass)
}

object EpisodeBaseWithDownloadInfo {
  private val FileNameDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")

  def mediaFileDateFormat(timestamp: DateTime): String =
    FileNameDateFormat.format(timestamp.toDate)

  def mediaFile(url: URL, podcast: URI, podcastTitle: String, episode: URI, episodeTitle: String, published: DateTime): String = {

    val MaxPathSegmentLength = 127

    def pathSegment(title: String, uri: URI, extension: String = ""): String = {
      val preferredName = title.fileNameEncoded + extension
      if (title.length > 0 && preferredName.length <= MaxPathSegmentLength) {
        preferredName
      } else {
        val fallbackOne = uri.toASCIIString.fileNameEncoded + extension
        if (fallbackOne.length <= MaxPathSegmentLength) {
          fallbackOne
        } else {
          val hash = Hash(uri.toString).fileNameEncoded
          val hostWithHash = s"${uri.getHost}/$hash".fileNameEncoded
          val path = uri.getPath.fileNameEncoded
          val fallback = hostWithHash + path.takeRight(MaxPathSegmentLength - hostWithHash.length)
          if (fallback.length <= MaxPathSegmentLength)
            fallback
          else
            hash + extension
        }
      }
    }

    val podcastDirName = pathSegment(podcastTitle, podcast)
    val extension = url.fileExtension.map(extension => s".$extension").getOrElse("").fileNameEncoded
    val fileName = pathSegment(episodeTitle + "_" + mediaFileDateFormat(published), episode, extension)

    s"$podcastDirName/$fileName"
  }
}