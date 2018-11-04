package mobi.upod.app.services.sync

import java.net.{URI, URL}

import mobi.upod.data.{Mapping, MappingProvider}
import org.joda.time.DateTime

final case class PodcastFetchInfo(url: URL, uri: Option[URI], title: Option[String], modified: Option[DateTime], eTag: Option[String], subscribed: Boolean, settings: Option[SubscriptionSettings])

object PodcastFetchInfo extends MappingProvider[PodcastFetchInfo] {

  def apply(url: URL): PodcastFetchInfo =
    PodcastFetchInfo(url, None, None, None, None, false, None)

  def apply(subscription: Subscription): PodcastFetchInfo =
    PodcastFetchInfo(subscription.url, None, None, None, None, true, Some(subscription.settings))

  import mobi.upod.data.Mapping._

  override val mapping: Mapping[PodcastFetchInfo] = map(
    "url" -> url,
    "uri" -> optional(uri),
    "title" -> optional(string),
    "modified" -> optional(dateTime),
    "eTag" -> optional(string),
    "subscribed" -> boolean,
    "settings" -> optional(SubscriptionSettings)
  )(apply)(unapply)
}