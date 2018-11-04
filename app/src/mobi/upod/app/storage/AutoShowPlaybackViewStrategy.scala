package mobi.upod.app.storage

object AutoShowPlaybackViewStrategy extends Enumeration {
  type AutoShowPlaybackViewStrategy = Value
  val Never = Value("Never")
  val Video = Value("Video")
  val Always = Value("Always")
}
