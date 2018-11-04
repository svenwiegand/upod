package mobi.upod.app.data

import java.net.URI
import mobi.upod.data.{Mapping, MappingProvider}

case class EpisodeId(podcast: URI, uri: URI) {

  override def toString: String = EpisodeId.toString(podcast, uri)
}

object EpisodeId extends MappingProvider[EpisodeId] {
  import Mapping._

  private val Pattern = "(.*)::(.*)".r

  def apply(id: String): EpisodeId = id match {
    case Pattern(podcast, episode) =>
      apply(podcast, episode)
    case _ =>
      throw new IllegalArgumentException(s"'$id' is no valid episode ID")
  }

  def apply(podcast: String, uri: String): EpisodeId =
    apply(new URI(podcast), new URI(uri))

  def toString(podcast: URI, uri: URI): String = s"$podcast::$uri"

  val mapping: Mapping[EpisodeId] = map(
    "podcast" -> uri,
    "uri" -> uri
  )(apply)(unapply)
}