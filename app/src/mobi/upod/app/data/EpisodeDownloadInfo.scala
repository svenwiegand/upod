package mobi.upod.app.data

import mobi.upod.data.MappingProvider

case class EpisodeDownloadInfo(
  listPosition: Option[Int],
  file: Option[String],
  complete: Boolean,
  fetchedBytes: Long,
  attempts: Int,
  lastErrorText: Option[String])

object EpisodeDownloadInfo extends MappingProvider[EpisodeDownloadInfo] {

  val default = EpisodeDownloadInfo(None, None, false, 0, 0, None)

  import mobi.upod.data.Mapping._

  val mapping = map(
    "listPosition" -> optional(int),
    "file" -> optional(string),
    "complete" -> boolean,
    "fetchedBytes" -> long,
    "attempts" -> int,
    "lastError_text" -> optional(string)
  )(apply)(unapply)
}