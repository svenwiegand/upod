package mobi.upod.app.data

import java.net.URL
import mobi.upod.data.{Mapping, MappingProvider}
import mobi.upod.app.storage.StorageProvider
import android.os.Environment
import java.io.File

case class Media(
  url: URL,
  mimeType: MimeType,
  length: Long,
  duration: Long) {

  def mimeTypeByFileExtension: Option[MimeType] =
    MimeType.forFileName(url.getPath)
}

object Media extends MappingProvider[Media] {

  import Mapping._

  val mapping = map(
    "url" -> url,
    "mimeType" -> MimeType.mapping,
    "length" -> long,
    "duration" -> long
  )(apply)(unapply)

  val jsonMapping = map(
    "url" -> url,
    "mimeType" -> MimeType.jsonMapping,
    "length" -> long,
    "duration" -> long
  )(apply)(unapply)
}
