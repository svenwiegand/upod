package mobi.upod.app.services.licensing

import org.joda.time.DateTime
import mobi.upod.data.{Mapping, MappingProvider}

private[services] case class License(licensed: Boolean, cacheExpiry: DateTime)

private[services] object License extends MappingProvider[License] {
  import Mapping._

  val mapping = map(
    "licensed" -> boolean,
    "cacheExpiry" -> dateTime
  )(apply)(unapply)
}