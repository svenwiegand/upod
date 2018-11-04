package mobi.upod.app.data

import mobi.upod.data.{Mapping, MappingProvider}
import org.joda.time.DateTime

case class PlaybackInfo(
  position: Long = 0,
  duration: Long = 0,
  finished: Boolean = false,
  speed: Option[Float] = None,
  gain: Option[Float] = None,
  modified: DateTime = new DateTime(0))

object PlaybackInfo extends MappingProvider[PlaybackInfo] {

  import Mapping._

  val default = PlaybackInfo()

  val mapping = map(
    "position" -> long,
    "duration" -> long,
    "finished" -> boolean,
    "speed" -> optional(float),
    "gain" -> optional(float),
    "modified" -> dateTime
  )(apply)(unapply)
}