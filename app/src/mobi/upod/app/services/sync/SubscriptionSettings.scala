package mobi.upod.app.services.sync

import mobi.upod.data.{Mapping, MappingProvider}

case class SubscriptionSettings(
  autoAddEpisodes: Boolean,
  autoAddToPlaylist: Boolean,
  autoDownload: Boolean,
  maxKeptEpisodes: Option[Int],
  playbackSpeed: Option[Float],
  volumeGain: Option[Float])

object SubscriptionSettings extends MappingProvider[SubscriptionSettings] {
  import Mapping._

  val default = SubscriptionSettings(false, false, false, None, None, None)

  val mapping = map(
    "autoAddEpisodes" -> boolean,
    "autoAddToPlaylist" -> boolean,
    "autoDownload" -> boolean,
    "maxKeptEpisodes" -> optional(int),
    "playbackSpeed" -> optional(float),
    "volumeGain" -> optional(float)
  )(apply)(unapply)
}
