package mobi.upod.app

import mobi.upod.data.{Mapping, MappingProvider}

final case class EpisodeCounterItem(
  areNew: Int,
  unfinished: Int,
  audio: Int,
  video: Int,
  downloaded: Int,
  finished: Int,
  starred: Int,
  playlist: Int,
  downloadQueue: Int)

object EpisodeCounterItem extends MappingProvider[EpisodeCounterItem] {

  import Mapping._

  val mapping = map(
    "isNew" -> int,
    "unfinished" -> int,
    "audio" -> int,
    "video" -> int,
    "downloaded" -> int,
    "finished" -> int,
    "starred" -> int,
    "playlist" -> int,
    "downloadQueue" -> int
  )(apply)(unapply)
}
