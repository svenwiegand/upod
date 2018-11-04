package mobi.upod.app.data

import java.net.URI
import mobi.upod.data.{Mapping, MappingProvider}
import org.joda.time.DateTime

case class PodcastReference(uri: URI, modified: DateTime)

object PodcastReference extends MappingProvider[PodcastReference] {

  import Mapping._

  val mapping = map(
    "uri" -> uri,
    "modified" -> dateTime
  )(apply)(unapply)
}
