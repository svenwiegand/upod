package mobi.upod.app.storage

object PlaybackNotificationButtons extends Enumeration {
  type PlaybackNotificationButtons = Value
  val StopPlaySkip = Value("StopPlaySkip")
  val StopPlayFastForward = Value("StopPlayFastForward")
  val RewindPlayFastForward = Value("RewindPlayFastForward")
}
