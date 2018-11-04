package mobi.upod.app.storage

import mobi.upod.data.{Mapping, MappingProvider}

case class EpisodeListHash(count: Long, hash: Long)

object EpisodeListHash extends MappingProvider[EpisodeListHash] {
  import Mapping._

  val mapping = map(
    "count" -> long,
    "hash" -> long
  )(apply)(unapply)
}