package mobi.upod.app.services.sync

import java.net.{URL, URI}
import mobi.upod.data.{Mapping, MappingProvider}
import org.joda.time.DateTime

case class Subscription(url: URL, settings: SubscriptionSettings)

object Subscription extends MappingProvider[Subscription] {

  def apply(url: URL): Subscription =
    new Subscription(url, SubscriptionSettings.default)

  import Mapping._

  val mapping = map(
    "url" -> url,
    "settings" -> SubscriptionSettings.mapping
  )(apply)(unapply)
}
