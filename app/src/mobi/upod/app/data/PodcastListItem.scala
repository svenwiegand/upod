package mobi.upod.app.data

import java.net.{URL, URI}
import mobi.upod.data.{Mapping, MappingProvider}
import mobi.upod.net.UriUtils

final case class PodcastListItem(
  id: Long,
  uri: URI,
  url: URL,
  title: String,
  categories: Set[Category],
  imageUrl: Option[URL],
  colors: Option[PodcastColors],
  subscribed: Boolean,
  syncError: Option[String],
  episodeCount: Int)
  extends PodcastBase

object PodcastListItem extends MappingProvider[PodcastListItem] {

  import Mapping._

  def apply(podcast: Podcast): PodcastListItem = apply(podcast.id, podcast.uri, podcast.url, podcast.title, podcast.categories, podcast.imageUrl, podcast.colors, podcast.subscribed, podcast.syncError, 0)

  val mapping = map(
    "id" -> long,
    "uri" -> uri,
    "url" -> url,
    "title" -> string,
    "imageUrl" -> optional(url),
    "colors" -> PodcastColors.optionalMapping,
    "subscribed" -> boolean,
    "syncError" -> optional(string),
    "episodeCount" -> int
  )(apply(_, _, _, _, Set(), _, _, _, _, _))(p => Some((p.id, p.uri, p.url, p.title, p.imageUrl, p.colors, p.subscribed, p.syncError, p.episodeCount)))
}
