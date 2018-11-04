package mobi.upod.android.media

import java.io.File
import android.media.MediaMetadataRetriever
import mobi.upod.android.logging.Logging

object MediaFileDurationRetriever extends Logging {

  def readDuration(file: File): Option[Long] = {
    try {
      val metaDataRetriever = new MediaMetadataRetriever
      metaDataRetriever.setDataSource(file.getAbsolutePath)
      val durationString = Option(metaDataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))
      val duration = durationString.map(_.toLong)
      log.info(s"retrieved duration of $duration for $file")
      duration
    } catch {
      case ex: Throwable =>
        log.info(s"failed to retrieve duration for $file", ex)
        None
    }

  }
}
