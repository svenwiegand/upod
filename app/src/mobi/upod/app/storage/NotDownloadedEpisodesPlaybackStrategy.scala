package mobi.upod.app.storage

object NotDownloadedEpisodesPlaybackStrategy extends Enumeration {
  type NotDownloadedEpisodesPlaybackStrategy = Value
  val Skip = Value("Skip")
  val StreamOnWifi = Value("StreamOnWifi")
  val Stream = Value("Stream")
}
