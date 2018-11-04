package mobi.upod.app.data

import java.net.URI
import mobi.upod.data.MappingProvider
import org.joda.time.DateTime

case class SubscriptionSettingsChange(uri: URI, timestamp: DateTime)

object SubscriptionSettingsChange extends MappingProvider[SubscriptionSettingsChange] {

  import mobi.upod.data.Mapping._

  val mapping = map(
    "uri" -> uri,
    "timestamp" -> dateTime
  )(apply)(unapply)
}