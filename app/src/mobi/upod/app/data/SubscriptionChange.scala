package mobi.upod.app.data

import java.net.URI
import mobi.upod.data.MappingProvider
import org.joda.time.DateTime

case class SubscriptionChange(uri: URI, subscribed: Boolean, timestamp: DateTime)

object SubscriptionChange extends MappingProvider[SubscriptionChange] {

  import mobi.upod.data.Mapping._

  val mapping = map(
    "uri" -> uri,
    "subscribed" -> boolean,
    "timestamp" -> dateTime
  )(apply)(unapply)
}