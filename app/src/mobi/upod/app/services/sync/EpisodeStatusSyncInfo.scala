package mobi.upod.app.services.sync

import java.net.{URI, URL}

import mobi.upod.app.data.{EpisodeReference, EpisodeStatus, PlaybackInfo}
import mobi.upod.data.{Mapping, MappingProvider}
import org.joda.time.DateTime

private[sync] case class EpisodeStatusSyncInfo(
  podcast: URL,
  uri: URI,
  status: EpisodeStatus.EpisodeStatus,
  statusModified: DateTime,
  playbackInfo: Option[PlaybackInfo])

private[sync] object EpisodeStatusSyncInfo extends MappingProvider[EpisodeStatusSyncInfo] {
  import Mapping._

  override val mapping: Mapping[EpisodeStatusSyncInfo] = map(
    "podcast" -> url,
    "uri" -> uri,
    "status" -> enumerated(EpisodeStatus),
    "statusModified" -> dateTime,
    "playbackInfo" -> optional(PlaybackInfo)
  )(apply)(unapply)
}