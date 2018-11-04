package mobi.upod.app.services.sync

import java.net.URI
import mobi.upod.data.{Mapping, MappingProvider}

case class PodcastSubscriptionSettings(podcast: URI, settings: SubscriptionSettings) {

  override def hashCode() = podcast.hashCode
}

object PodcastSubscriptionSettings extends MappingProvider[PodcastSubscriptionSettings] {

  import mobi.upod.data.Mapping._

  val mapping: Mapping[PodcastSubscriptionSettings] = map(
    "podcast" -> uri,
    "settings" -> SubscriptionSettings.mapping
  )(apply)(unapply)
}
