package mobi.upod.app.data

import java.net.URL

import mobi.upod.data.MappingProvider

case class EpisodePodcastInfo(
  id: Long,
  url: URL,
  title: String,
  imageUrl: Option[URL],
  colors: Option[PodcastColors]
)

object EpisodePodcastInfo extends MappingProvider[EpisodePodcastInfo] {
  import mobi.upod.data.Mapping._

  def apply(): EpisodePodcastInfo =
    EpisodePodcastInfo(0, null, "", None, None)

  val mapping = map(
    "id" -> long,
    "url" -> url,
    "title" -> string,
    "imageUrl" -> optional(url),
    "colors" -> PodcastColors.optionalMapping
  )(apply)(unapply)
}