package mobi.upod.app.data

object EpisodeStatus extends Enumeration {
  type EpisodeStatus = Value
  val New = Value("new")
  val Library = Value("library")
  val Starred = Value("starred")
  val Finished = Value("finished")
  val NoLongerAvailable = Value("noLongerAvailable")
}
