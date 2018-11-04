package mobi.upod.app.services.playback

object RemotePlaybackState extends Enumeration {
  type RemotePlaybackState = Value
  val Unknown = Value("Unknown")
  val Idle = Value("Idle")
  val Buffering = Value("Buffering")
  val Paused = Value("Paused")
  val Playing = Value("Playing")
}
