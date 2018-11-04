package mobi.upod.app.data

import mobi.upod.data.MappingProvider

case class PlaylistChange(episodeId: Long, position: Option[Int])

object PlaylistChange extends MappingProvider[PlaylistChange] {

  import mobi.upod.data.Mapping._

  val mapping = map(
    "episodeId" -> long,
    "position" -> optional(int)
  )(apply)(unapply)
}
