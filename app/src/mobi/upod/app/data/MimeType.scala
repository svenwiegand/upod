package mobi.upod.app.data

import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndEnclosure
import mobi.upod.data.{Mapping, MappingProvider}
import mobi.upod.io.Path

case class MimeType(mediaType: String, subType: Option[String]) {
  import MimeType._

  val caseInsensitiveMediaType = mediaType.toLowerCase
  val isAudio = caseInsensitiveMediaType == Audio || caseInsensitiveMediaType == XAudio
  val isVideo = caseInsensitiveMediaType == Video || caseInsensitiveMediaType == XVideo
  val isPlayable = isAudio || isVideo

  override def toString: String = mediaType + subType.map("/" + _).getOrElse("")
}

object MimeType extends MappingProvider[MimeType] {
  val Audio = "audio"
  val XAudio = "x-audio"
  val Video = "video"
  val XVideo = "x-video"
  val TypeByExtension = Map(
    "3gp" -> "video/3gp",
    "aac" -> "audio/aac",
    "flac" -> "audio/flac",
    "imy" -> "audio/midi",
    "m3u" -> "audio/x-mpequrl",
    "mid" -> "audio/midi",
    "midi" -> "audio/midi",
    "m4a" -> "audio/mp4",
    "mkv" -> "audio/midi",
    "mp3" -> "audio/mpeg3",
    "mp4" -> "video/mp4",
    "mpa" -> "video/mpeg",
    "mpg" -> "video/mpeg",
    "mxmf" -> "audio/midi",
    "my" -> "audio/make",
    "ogg" -> "audio/ogg",
    "ota" -> "audio/midi",
    "rtttl" -> "audio/midi",
    "rtx" -> "audio/midi",
    "ts" -> "audio/mp2t",
    "wav" -> "audio/wav",
    "webm" -> "video/webm",
    "xmf" -> "audio/midi"
  )

  def apply(mimeType: String): MimeType = mimeType.split('/').toList match {
    case mediaType :: subTypes => MimeType(mediaType, subTypes.headOption)
  }

  def apply(enclosure: SyndEnclosure): Option[MimeType] = enclosure.getType match {
    case mimeType: String => Some(apply(mimeType))
    case _ if enclosure.getUrl != null => forFileName(enclosure.getUrl)
    case _ => None
  }

  def forFileName(fileName: String): Option[MimeType] =
    fileName.fileExtension.flatMap(TypeByExtension.get(_).map(apply))

  import Mapping.{Mapping => Mappng, _}

  val mapping = map(
    "mediaType" -> string,
    "subType" -> optional(string)
  )(apply)(unapply)

  val jsonMapping = Mappng[MimeType] {
    element => MimeType(element.asDataPrimitive.asString)
  } {
    (factory, name, element) => factory.create(name, element.toString)
  }
}
