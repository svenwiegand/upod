package mobi.upod.app.data

import java.net.{URI, URL}

import mobi.upod.data.MappingProvider

case class EpisodeReference(podcast: URL, uri: URI)

object EpisodeReference extends MappingProvider[EpisodeReference] {

  import mobi.upod.data.Mapping._

  val mapping = map(
    "podcastInfo_url" -> url,
    "uri" -> uri
  )(apply)(unapply)

  val jsonMapping = map(
    "podcast" -> url,
    "uri" -> uri
  )(apply)(unapply)
}