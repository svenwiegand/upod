package mobi.upod.app.data

import mobi.upod.data.{Mapping, MappingProvider}
import java.net.URI
import org.joda.time.DateTime

case class EpisodeListChange(uri: URI, state: EpisodeStatus.EpisodeStatus, timestamp: DateTime)

object EpisodeListChange extends MappingProvider[EpisodeListChange]  {
  import Mapping._

  val mapping = map(
    "uri" -> uri,
    "change" -> enumerated(EpisodeStatus),
    "timestamp" -> dateTime
  )(apply)(unapply)
}