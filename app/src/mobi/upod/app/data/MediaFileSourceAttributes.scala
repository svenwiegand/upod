package mobi.upod.app.data

import java.net.{URI, URL}
import org.joda.time.DateTime
import mobi.upod.data.{Mapping, MappingProvider}

case class MediaFileSourceAttributes(
  url: URL,
  podcast: URI,
  podcastTitle: String,
  episode: URI,
  episodeTitle: String,
  published: DateTime
) {

  def episodeId = EpisodeId(podcast, episode)

  def file: String =
    EpisodeBaseWithDownloadInfo.mediaFile(url, podcast, podcastTitle, episode, episodeTitle, published)
}

object MediaFileSourceAttributes extends MappingProvider[MediaFileSourceAttributes] {
  import Mapping._

  override val mapping = map(
    "media_url" -> url,
    "podcast" -> uri,
    "podcastInfo_title" -> string,
    "uri" -> uri,
    "title" -> string,
    "published" -> dateTime
  )(apply)(unapply)
}
