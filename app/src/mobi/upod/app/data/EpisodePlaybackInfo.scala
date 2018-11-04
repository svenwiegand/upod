package mobi.upod.app.data

import mobi.upod.data.MappingProvider
import org.joda.time.DateTime

case class EpisodePlaybackInfo(listPosition: Option[Int],
                               finished: Boolean,
                               playbackPosition: Long,
                               playbackSpeed: Option[Float],
                               volumeGain: Option[Float],
                               playbackPositionTimestamp: DateTime)

object EpisodePlaybackInfo extends MappingProvider[EpisodePlaybackInfo] {
  import mobi.upod.data.Mapping._

  val default = EpisodePlaybackInfo(None, false, 0, None, None, new DateTime(0))

  val mapping = map(
    "listPosition" -> optional(int),
    "finished" -> boolean,
    "playbackPosition" -> long,
    "playbackSpeed" -> optional(float),
    "volumeGain" -> optional(float),
    "playbackPositionTimestamp" -> dateTime
  )(apply)(unapply)
}