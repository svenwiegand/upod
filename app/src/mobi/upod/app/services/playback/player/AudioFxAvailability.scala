package mobi.upod.app.services.playback.player

object AudioFxAvailability extends Enumeration {
  type AudioFxAvailability = Value

  /** Not supported on this platform */
  val NotSupported = Value

  /** Not for the currently selected player engine, but user could switch player to get it */
  val NotForCurrentPlayer = Value

  /** For the currently playing media audio effects are never available */
  val NotForCurrentDataSource = Value

  /** Possible in principle, but not at the moment (maybe streaming?) */
  val NotNow = Value

  /** Audio effects are available */
  val Available = Value
}
